package bsearch.evaluation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;
import bsearch.nlogolink.ModelRunningService;
import bsearch.nlogolink.MultipleRunResult;
import bsearch.representations.Chromosome;


public class SearchManager 
{
	private final ModelRunningService modelRunningService;
	private final SearchProtocolInfo protocol;  
	private final ObjectiveEvaluator objectiveEvaluator;
	
	private final SearchProgressStatsKeeper statsKeeper;
	
	private Map<Chromosome, List<Object>> objectivesCache;
	
	public SearchManager(int searchIDNumber, ModelRunningService runner, SearchProtocolInfo protocol, 
			ObjectiveEvaluator fitnessFunction,  SearchProgressStatsKeeper statsKeeper)
	{
		this.modelRunningService = runner;
		this.protocol = protocol;
		this.objectiveEvaluator = fitnessFunction;
		this.statsKeeper = statsKeeper;

		//TODO: If cache size is limited, use LRUCache in place of LinkedHashMap there...
		this.objectivesCache = Collections.synchronizedMap(new LinkedHashMap<Chromosome,List<Object>>());
	}

	@Deprecated
	public double computeFitnessSingleLegacy(Chromosome point, int numReplicationsDesired, MersenneTwisterFast rng) throws ModelRunnerException, BehaviorSearchException, InterruptedException
	{
		MOEASolutionWrapper dummyWrapper = MOEASolutionWrapper.getDummySolutionWrapper(point, protocol.objectives);
		computeFitnessSingle(dummyWrapper, numReplicationsDesired, rng);
		return dummyWrapper.getSolution().getObjective(0);
	}

	@Deprecated
	public double[] computeFitnessBatchLegacy(Chromosome[] points, int numReplicationsDesired, MersenneTwisterFast rng)
			throws ModelRunnerException, NetLogoLinkException, InterruptedException, BehaviorSearchException {
		double[] firstObjectiveVals = new double[points.length];
		for (int i = 0; i < points.length; i++) {
			firstObjectiveVals[i] = computeFitnessSingleLegacy(points[i], numReplicationsDesired, rng);
		}
		return firstObjectiveVals;
	}

	/**
	 * In addition to computing the fitness for this point, this method also checks if it's the best so far,
	 * in which case it's stored as the current best.
	 */
	public void computeFitnessSingle(MOEASolutionWrapper solutionWrapper, int numReplicationsDesired, MersenneTwisterFast rng) throws ModelRunnerException, BehaviorSearchException, InterruptedException
	{
		Chromosome point = solutionWrapper.getPoint();
		List<Object> cachedResult = this.objectivesCache.get(point);
		if (cachedResult != null) {
			solutionWrapper.setObjectivesOnSolution(cachedResult);
			return;
		}

		HashMap<Chromosome, Integer> desiredRuns = objectiveEvaluator.getRunsNeeded(point, numReplicationsDesired);

		Map<Chromosome,MultipleRunResult> resultsMap = modelRunningService.doBatchRun(desiredRuns,rng);
		List<Object> objectiveValsForThisPoint = objectiveEvaluator.evaluateAllObjectives(point, resultsMap);
		if (protocol.searchAlgorithmInfo.caching) {
			// NOTE: There is a race condition where two threads could both enter this method before the point is cached 
			// both call evaluateAllObjectives, and both attempt to store their in the cache.
			// In this case, since we use putIfAbsent, the cache will simply hold the first value that was stored. 
			// The second evaluation was wasted effort, but this is unlikely to occur often, and the alternative of 
			// synchronizing over this whole section of code would defeat the gains from parallel evaluation. 
			objectivesCache.putIfAbsent(point, objectiveValsForThisPoint);
		}
		solutionWrapper.setObjectivesOnSolution(objectiveValsForThisPoint);
		// at this point, we're done with evaluation as far as MOEA is concerned, 
		// but we need to handle logging, tracking the best found, and best "rechecking" 
		
		// get a new solution with a new wrapper, so that nothing we do below will impact the original solution that MOEA uses
		solutionWrapper = solutionWrapper.copy(); 
		
		boolean newBest = false;
		int modelRunCount = 0;
		
		synchronized (statsKeeper) {
			modelRunCount = statsKeeper.getModelRunCounter();
			newBest = statsKeeper.maybeUpdateBest(solutionWrapper.getSolution());

			for (Chromosome runPoint : resultsMap.keySet()) {
				for (SingleRunResult runResult : resultsMap.get(runPoint).getSingleRuns()) {
					statsKeeper.modelRunEvent(runResult, false);
					modelRunCount++;
				}
			}
			solutionWrapper.setSearchID(statsKeeper.getSearchID());
			solutionWrapper.setModelRunCounter(modelRunCount);
			statsKeeper.fitnessComputedEvent(solutionWrapper);
			if (newBest && !protocol.modelDCInfo.useBestChecking()){
				statsKeeper.newBestEvent(solutionWrapper);
			}
		}

		if (newBest && protocol.modelDCInfo.useBestChecking()) {
			MOEASolutionWrapper recheckedSolutionWrapper = solutionWrapper.copy();
			computeFitnessForRechecking(recheckedSolutionWrapper, protocol.modelDCInfo.bestCheckingNumReplications, rng);
			// pair these two wrappers, so we can access the other when we need to get stats on them much later.
			solutionWrapper.setCheckingPairWrapper(recheckedSolutionWrapper); 
			recheckedSolutionWrapper.setCheckingPairWrapper(solutionWrapper);
			
			synchronized (statsKeeper) {
				statsKeeper.maybeUpdateCheckedBest(recheckedSolutionWrapper.getSolution());
				statsKeeper.newBestEvent(solutionWrapper);
			}			
		}
	}
	
	private void computeFitnessForRechecking(MOEASolutionWrapper solutionWrapper, int numReplicationsDesired, MersenneTwisterFast rng) 
		throws ModelRunnerException, NetLogoLinkException, InterruptedException, BehaviorSearchException
	{
		// We create an auxilliaryRNG to use for "best-checking", so that the number of best-checking
		// replicates doesn't affect the search process at all. 
		MersenneTwisterFast auxilliaryRNG = new MersenneTwisterFast(rng.clone().nextInt());

		Chromosome pointOfInterest = solutionWrapper.getPoint();
		HashMap<Chromosome, Integer> desiredRuns = objectiveEvaluator.getRunsNeeded(pointOfInterest, numReplicationsDesired);

		Map<Chromosome,MultipleRunResult> resultsMap = modelRunningService.doBatchRun(desiredRuns,auxilliaryRNG);
				
		solutionWrapper.setObjectivesOnSolution(objectiveEvaluator.evaluateAllObjectives(pointOfInterest, resultsMap));

		
		synchronized (statsKeeper) {
			for (Chromosome runPoint : resultsMap.keySet()) {
				for (SingleRunResult runResult : resultsMap.get(runPoint).getSingleRuns()) {
					statsKeeper.modelRunEvent(runResult, true);
				}
			}

		}

	}
	
	public boolean searchFinished()
	{
		return statsKeeper.getModelRunCounter() >= protocol.searchAlgorithmInfo.evaluationLimit;
//		return searchHasTotallyStalled() || evaluationCounter >= protocol.searchAlgorithmInfo.evaluationLimit;
	}
	public int getRemainingEvaluations()
	{
		return protocol.searchAlgorithmInfo.evaluationLimit - statsKeeper.getModelRunCounter();
	}
	 
	public ObjectiveEvaluator getObjectiveEvaluator()
	{
		return objectiveEvaluator;
	}
	public SearchProgressStatsKeeper getStatsKeeper() {
		return statsKeeper;
	}

	public boolean fitnessStrictlyBetter(double f1, double f2)
	{
		return objectiveEvaluator.strictlyBetterThan(f1, f2);
	}	
	

	// TODO: This method probably doesn't belong here... refactor to a cleaner design? 
	public Chromosome tournamentSelect( Chromosome[] population ,
			double[] fitness , int tournamentSize, MersenneTwisterFast rng)
	{
		//select several distinct random indices into the population array 
		int[] randomIndices = new int[tournamentSize];
		int i = 0;
		while (i < randomIndices.length)
		{
			int randomIndex = rng.nextInt(population.length);
			for (int j = 0; j < i; j++)
			{
				if (randomIndices[j] == randomIndex)
					continue;
			}
			randomIndices[i] = randomIndex;
			i++;
		}
		
		int bestIndex = -1;
		double bestFitness = objectiveEvaluator.getWorstConceivableFitnessValue() ;
		for (i = 0 ; i < randomIndices.length; i++)
		{
			double f = fitness[randomIndices[i]];
			if (objectiveEvaluator.strictlyBetterThan(f,bestFitness))
			{
				bestIndex = randomIndices[i];
				bestFitness = f;
			}			
		}
		return population[bestIndex];		
	}
}