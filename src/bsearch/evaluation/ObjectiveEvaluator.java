package bsearch.evaluation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.datamodel.ObjectiveFunctionInfo.OBJECTIVE_TYPE;
import bsearch.nlogolink.MultipleRunResult;
import bsearch.representations.Chromosome;
import bsearch.representations.DummyChromosome;


public strictfp class ObjectiveEvaluator
{
	private final List<ObjectiveFunctionInfo> objectiveFunctions;
	
	public ObjectiveEvaluator(List<ObjectiveFunctionInfo> objectiveFunctions) throws BehaviorSearchException
	{
		this.objectiveFunctions = objectiveFunctions;

		// TODO: Move this error checking somewhere else -- maybe ProtocolInfo.sanityCheck?
		for (ObjectiveFunctionInfo objInfo : objectiveFunctions) {
			if 	(objInfo.useDerivative() && objInfo.fitnessDerivativeDelta == 0) {
				throw new BehaviorSearchException("When taking the 'derivative' of the fitness function with respect to parameter X, the delta value (change in X) cannot be 0!");
			}
		}
	}
	private static Chromosome getNeighborDeltaAway(Chromosome point, ObjectiveFunctionInfo objectiveInfo) throws BehaviorSearchException
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
	
	/** Given a point in the search space, and a requested number of repetitions for that point, return a mapping which
	 * contains all the points that would need to be evaluated in order to compute the fitness at the given point, and
	 * how many repetitions (runs) are needed at each of those points.
	 * 
	 */
	public HashMap<Chromosome, Integer> getRunsNeeded(Chromosome point, int repetitionsRequested) throws BehaviorSearchException
	{
		LinkedHashMap<Chromosome, Integer> map = new LinkedHashMap<Chromosome,Integer>(1);
		map.put(point, repetitionsRequested);
		
		for (ObjectiveFunctionInfo objInfo : objectiveFunctions) {
			if (objInfo.useDerivative()) {
				Chromosome neighborPoint = getNeighborDeltaAway(point, objInfo);  
				map.put(neighborPoint, repetitionsRequested);
			}
		}

		return map;
	}
	
	/**
	 * How many runs would be needed if caching were turned off 
	 * (or if caching is on, but none of the needed runs had been done previously)
	 */
	public int getMaximumRunsThatCouldBeNeeded(int repetitionsRequested)
	{
		int maxNeeded = repetitionsRequested;
		for (ObjectiveFunctionInfo objInfo : objectiveFunctions) {
			if (objInfo.useDerivative()) {
				maxNeeded += repetitionsRequested;
			}
		}
		return maxNeeded;
	}
	
	public List<Object> evaluateAllObjectives(Chromosome point, Map<Chromosome,MultipleRunResult> resultsMap) throws BehaviorSearchException
	{	
		List<Object> combinedValsAtPoint = resultsMap.get(point).getCombinedMeasures();
		
		for (int i = 0; i < objectiveFunctions.size(); i++) {
			ObjectiveFunctionInfo objInfo = objectiveFunctions.get(i);

			// error checking
			Object objValAtPoint = combinedValsAtPoint.get(i);
			if ((objInfo.objectiveType == OBJECTIVE_TYPE.MINIMIZE || objInfo.objectiveType == OBJECTIVE_TYPE.MAXIMIZE)
					&& (!(objValAtPoint instanceof Double))) {
				throw new BehaviorSearchException("Objective " + objInfo.name + " must report a number, but reported " + objValAtPoint + " instead!");
			}
			// possibly check NOVELTY here to make sure it's a double[] array?
			
			if (objInfo.useDerivative()) {
				double pointVal = (double)objValAtPoint;
				Chromosome neighbor = getNeighborDeltaAway( point, objInfo );
					
				List<Object> combinedValsAtNeighbor = resultsMap.get( neighbor ).getCombinedMeasures();
				double neighborPointVal = (double) combinedValsAtNeighbor.get(i);
				double denominator = objInfo.fitnessDerivativeDelta;
				double derivObjectiveVal = (pointVal - neighborPointVal) / denominator;
				if (objInfo.fitnessDerivativeUseAbs) {
					derivObjectiveVal = StrictMath.abs(derivObjectiveVal);
				}
				combinedValsAtPoint.set(i, derivObjectiveVal);
			}
		}
		
		return combinedValsAtPoint;
	}

	public boolean strictlyBetterThan(double v1, double v2)
	{
		return v1 < v2;
	}

	/** 
	 * @return a fitness value that is worse than anything that could actually be obtained by evaluating an individual.
	 */
	public double getWorstConceivableFitnessValue() {
		return Double.POSITIVE_INFINITY;
	}

	

}