package bsearch.evaluation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.nlogolink.ModelRunResult;
import bsearch.nlogolink.ModelRunningService;
import bsearch.representations.Chromosome;
import bsearch.representations.DummyChromosome;


/**
 * This class is used to compute fitness based on an approximation of the derivative
 * of the objective function -- i.e. how much change there is in the objective
 * function as one of the parameters is varied...
 */
public strictfp class DerivativeFitnessFunction implements FitnessFunction
{
	private final ObjectiveFunctionInfo objectiveInfo;
	
	
	public DerivativeFitnessFunction(ObjectiveFunctionInfo objectiveInfo) throws BehaviorSearchException
	{
		this.objectiveInfo = objectiveInfo;
		if 	(objectiveInfo.fitnessDerivativeDelta == 0)
		{
			throw new BehaviorSearchException("When taking the 'derivative' of the fitness function with respect to parameter X, the delta value (change in X) cannot be 0!");
		}

	}
	private Chromosome getPointDeltaNearby(Chromosome point) throws BehaviorSearchException
	{
		LinkedHashMap<String, Object> newParamSettings = new LinkedHashMap<String,Object>(point.getParamSettings());
		
		Object curVal = newParamSettings.get(objectiveInfo.fitnessDerivativeParameter);
		if (curVal instanceof Number)
		{
			double val = ((Number) curVal).doubleValue();
			double newVal = (val - objectiveInfo.fitnessDerivativeDelta); 
			newParamSettings.put(objectiveInfo.fitnessDerivativeParameter, newVal);
		}
		else
		{
			throw new BehaviorSearchException("Derivative-based fitness measurements currently only work with numerical parameters!");
		}
		return new DummyChromosome(point.getSearchSpace(), newParamSettings);
	}

	public HashMap<Chromosome, Integer> getRunsNeeded(Chromosome point, int repetitionsRequested, ResultsArchive archive) throws BehaviorSearchException
	{
		LinkedHashMap<Chromosome, Integer> map = new LinkedHashMap<Chromosome,Integer>(1);
		map.put(point, StrictMath.max(0, repetitionsRequested - archive.getResultsCount(point)));
		Chromosome deltaComparePoint = getPointDeltaNearby(point);  
		map.put(deltaComparePoint, StrictMath.max(0, repetitionsRequested - archive.getResultsCount(deltaComparePoint)));
		return map;
	}
	public int getMaximumRunsThatCouldBeNeeded(int repetitionsRequested)
	{
		return 2 * repetitionsRequested;
	}
	
	public double evaluate(Chromosome point, ResultsArchive archive, ModelRunningService runService) throws BehaviorSearchException
	{	
		List<ModelRunResult> pointResults = archive.getResults( point );
		double pointVal = (double) runService.getCombinedResultsForEachObjective(pointResults).get(0);
		
		Chromosome neighbor = getPointDeltaNearby( point );
		
		List<ModelRunResult> neighborResults = archive.getResults( neighbor );	
		double nieghborPointVal = (double) runService.getCombinedResultsForEachObjective(neighborResults).get(0);

		double denominator = objectiveInfo.fitnessDerivativeDelta;
				
		if (objectiveInfo.fitnessDerivativeUseAbs)
		{
			return StrictMath.abs((pointVal - nieghborPointVal) / denominator);
		}
		else
		{
			return (pointVal - nieghborPointVal) / denominator;
		}
	}

	public double compare(double v1, double v2)
	{
		if (objectiveInfo.fitnessMinimized)
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

	public double getWorstConceivableFitnessValue() {
		return objectiveInfo.fitnessMinimized?Double.POSITIVE_INFINITY:Double.NEGATIVE_INFINITY;
	}
	public double getBestConceivableFitnessValue() {
		return objectiveInfo.fitnessMinimized?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY;
	}

	

}