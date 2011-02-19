package bsearch.evaluation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import bsearch.app.BehaviorSearchException;
import bsearch.app.SearchProtocol;
import bsearch.nlogolink.ModelRunResult;
import bsearch.representations.Chromosome;
import bsearch.space.SearchSpace;


/**
 * This class is a work-in-progress.  Currently unused...
 */
public strictfp class DerivativeFitnessFunction implements FitnessFunction
{
	private final SearchProtocol protocol;
	private final String paramName;
	private final double deltaDistance; 
	
	public DerivativeFitnessFunction(SearchProtocol protocol, String paramName, double deltaDistance)
	{
		this.protocol = protocol;
		this.paramName = paramName;
		this.deltaDistance = deltaDistance;
	}
	private Chromosome getPointDeltaNearby(SearchSpace space, Chromosome point) throws BehaviorSearchException
	{
		LinkedHashMap<String, Object> newParamSettings = new LinkedHashMap<String,Object>(point.getParamSettings());
		
		Object curVal = newParamSettings.get(paramName);
		if (curVal instanceof Number)
		{
			double val = ((Number) curVal).doubleValue();
			double newVal = (val - deltaDistance); 
			newParamSettings.put(paramName, newVal);
		}
		else
		{
			throw new BehaviorSearchException("Derivative-based fitness measurements only work with numerical parameters!");
		}
		return null;
	}

	public HashMap<Chromosome, Integer> getRunsNeeded(Chromosome point, int repetitionsRequested, ResultsArchive archive)
	{
		LinkedHashMap<Chromosome, Integer> map = new LinkedHashMap<Chromosome,Integer>(1);
		map.put(point, StrictMath.max(0, repetitionsRequested - archive.getResultsCount(point)));
		//TODO: Fix, by adding deriv point to map too
		return map;
	}
	public int getMaximumRunsThatCouldBeNeeded(Chromosome point, int repetitionsRequested, ResultsArchive archive)
	{
		return 2 * repetitionsRequested;
	}
	
	public double evaluate(Chromosome point, ResultsArchive archive)
	{		
		List<ModelRunResult> resultsSoFar = archive.getResults( point );
		
		LinkedList<Double> condensedResults = new LinkedList<Double>();
		for (ModelRunResult result: resultsSoFar)
		{
			List<Double> singleRunHistory = result.getPrimaryTimeSeries();
			double dResult = protocol.fitnessCollecting.collectFrom(singleRunHistory);
			
			condensedResults.add(dResult);
		}
		return protocol.fitnessCombineReplications.combine(condensedResults);
	}

	public double compare(double v1, double v2)
	{
		if (protocol.fitnessMinimized)
		{
			return v2 - v1 ;
		}
		else
		{
			return v1 - v2 ;
		}
	}
	public boolean strictlyBetterThan(double v1, double v2)
	{
		return compare(v1,v2) > 0.0;
	}

	public double getWorstPossibleFitnessValue() {
		return protocol.fitnessMinimized?Double.POSITIVE_INFINITY:Double.NEGATIVE_INFINITY;
	}
	public double getBestPossibleFitnessValue() {
		return protocol.fitnessMinimized?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
	}

	public boolean reachedStopGoalFitness(double fitness) {
		return false;  //TODO: FIXME (see StandardFitnessFunction)
		/*if (!hasStopGoal)
		{
			return false;
		}
		else
		{
			if (protocol.fitnessMinimized)
			{ 
				return fitness <= stopGoalFitness;
			}
			else
			{
				return fitness >= stopGoalFitness;
			}
		}*/
	}
	

}