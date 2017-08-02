package bsearch.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nlogo.api.MersenneTwisterFast;


import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;
import bsearch.nlogolink.ModelRunningService;
import bsearch.representations.Chromosome;


public class SearchManager 
{
	private final ModelRunningService modelRunningService;
	private final SearchProtocolInfo protocol;  
	private final ObjectiveEvaluator fitnessFunction;
	
	private final SearchProgressStatsKeeper statsKeeper;
	
	private Map<Chromosome, List<Object>> objectivesCache;

	private List<ResultListener> resultListeners = new ArrayList<ResultListener>();
	
	public SearchManager(int searchIDNumber, ModelRunningService runner, SearchProtocolInfo protocol, 
			ObjectiveEvaluator fitnessFunction )
	{
		this.modelRunningService = runner;
		this.protocol = protocol;
		this.fitnessFunction = fitnessFunction;
		this.objectivesCache = new LinkedHashMap<Chromosome,List<Object>>();

		this.statsKeeper = new SearchProgressStatsKeeper(searchIDNumber,0,0,protocol.objectives);
	}

	public void addResultsListener(ResultListener listener)
	{
		resultListeners.add(listener);
	}

	/**
	 * Note that this method *will* affect the state of the RNG, so you may wish to pass in a cloned copy of your RNG, 
	 *  or a brand new RNG, rather than an RNG that you are using for other purposes...
	 * @param pointOfInterest
	 * @param numReplicationsDesired
	 * @param rng 
	 * @return
	 * @throws ModelRunnerException
	 * @throws BehaviorSearchException
	 * @throws InterruptedException
	 */
	private List<Object> computeFitnessWithoutSideEffects(Chromosome pointOfInterest, int numReplicationsDesired, MersenneTwisterFast rng) 
		throws ModelRunnerException, NetLogoLinkException, InterruptedException, BehaviorSearchException
	{
		HashMap<Chromosome, Integer> desiredRuns = fitnessFunction.getRunsNeeded(pointOfInterest, numReplicationsDesired);

		Map<Chromosome,List<SingleRunResult>> resultsMap = modelRunningService.doBatchRun(desiredRuns,rng);
		statsKeeper.setModelRunRecheckingCounter(statsKeeper.getModelRunRecheckingCounter() + numReplicationsDesired*resultsMap.size()); 
		
		return fitnessFunction.evaluateAllObjectives(pointOfInterest, resultsMap, modelRunningService);
	}

	public double computeFitnessSingle(Chromosome point, int numReplicationsDesired, MersenneTwisterFast rng) throws ModelRunnerException, BehaviorSearchException, InterruptedException
	{
		Chromosome[] points = new Chromosome[] { point };
		return computeFitnessBatch(points, numReplicationsDesired, rng)[0];
	}
	/**
	 * In addition to computing the fitness for this point, this method also checks if it's the best so far,
	 * in which case it's stored as the current best.
	 */
	public double[] computeFitnessBatch(Chromosome[] points, int numReplicationsDesired, MersenneTwisterFast rng) 
		throws ModelRunnerException, NetLogoLinkException, InterruptedException, BehaviorSearchException
	{
		HashMap<Chromosome, Integer> allDesiredRuns = new LinkedHashMap<Chromosome,Integer>();
		
		// as we add replications to be evaluated, keep track of how many are added for each point that we're 
		// computing fitness for.
		int[] numReplicationsNeeded = new int[points.length];
		
		for (int i = 0; i < points.length; i++)
		{
			if (this.objectivesCache.containsKey(points[i])) {
				numReplicationsNeeded[i] = 0; // use cached objectives...
			} else {
				HashMap<Chromosome, Integer> desiredRuns = fitnessFunction.getRunsNeeded(points[i], numReplicationsDesired);
				allDesiredRuns.putAll(desiredRuns);
				numReplicationsNeeded[i] = desiredRuns.values().stream().mapToInt(num -> num.intValue()).sum();				
			}
		}

		Map<Chromosome,List<SingleRunResult>> resultsMap = modelRunningService.doBatchRun(allDesiredRuns,rng);
		
		// temporarily count up model run counter for notifying modelRun listeners
		// then we'll count it up again (using statsKeeper) below for the objective listeners
		int tempModelRunCounter = statsKeeper.getModelRunCounter() ;
		for (Chromosome point : resultsMap.keySet()) {
			for (SingleRunResult runResult : resultsMap.get(point)) {
				tempModelRunCounter++;
				for (ResultListener listener : resultListeners) {
					listener.modelRunOccurred(statsKeeper.getSearchIDNumber(), tempModelRunCounter, 
									statsKeeper.getModelRunRecheckingCounter(), runResult);
				}
			}
		}

		// We create an auxilliaryRNG to use for "best-checking", so that the number of best-checking
		// replicates doesn't affect the search process at all. 
		MersenneTwisterFast auxilliaryRNG = new MersenneTwisterFast(rng.clone().nextInt());

		double[] firstSlotFitnesses = new double[points.length]; 
		for (int i = 0; i < points.length; i++)
		{
			List<Object> fitnessObj = objectivesCache.get(points[i]);
			if (fitnessObj == null) { // if not in cache, evaluate objectives (and store for later, if caching turned on)
				fitnessObj = fitnessFunction.evaluateAllObjectives(points[i], resultsMap, modelRunningService);
				if (protocol.searchAlgorithmInfo.caching) {
					objectivesCache.put(points[i], fitnessObj);
				}
			}
			//TODO: FIXME for MOEA
			double[] fitness = toPrimitiveDoubleArray(fitnessObj);
			firstSlotFitnesses[i] = fitness[0];
			statsKeeper.setModelRunCounter(statsKeeper.getModelRunCounter() + numReplicationsNeeded[i]);
			
			if (statsKeeper.checkIfNewBest(points[i],fitness))
			{
				if (protocol.searchAlgorithmInfo.useBestChecking())
				{
					List<Object> bestCheckedObj = computeFitnessWithoutSideEffects(points[i], protocol.searchAlgorithmInfo.bestCheckingNumReplications, auxilliaryRNG);
					double[] bestChecked = toPrimitiveDoubleArray(bestCheckedObj);
					statsKeeper.setCurrentBestFitnessCheckedEstimate(bestChecked); 
				}
				for (ResultListener listener : resultListeners)
				{
					listener.newBestFound(this.statsKeeper);
				}
			}
			if (numReplicationsNeeded[i] > 0) // i.e. we have new information
			{
				for (ResultListener listener : resultListeners)
				{
					listener.fitnessComputed(this.statsKeeper, points[i].getParamSettings(), fitness);
				}
			}
		}
		return firstSlotFitnesses;
	}
	
	private static double[] toPrimitiveDoubleArray(List<Object> list) {
		double[] nums = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			nums[i] = (double) list.get(i);
		}
		return nums;
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
	 
	public ObjectiveEvaluator getFitnessFunction()
	{
		return fitnessFunction;
	}
	public SearchProgressStatsKeeper getStatsKeeper() {
		return statsKeeper;
	}

	public boolean fitnessStrictlyBetter(double f1, double f2)
	{
		return fitnessFunction.strictlyBetterThan(f1, f2);
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
		double bestFitness = fitnessFunction.getWorstConceivableFitnessValue() ;
		for (i = 0 ; i < randomIndices.length; i++)
		{
			double f = fitness[randomIndices[i]];
			if (fitnessFunction.strictlyBetterThan(f,bestFitness))
			{
				bestIndex = randomIndices[i];
				bestFitness = f;
			}			
		}
		return population[bestIndex];		
	}
}