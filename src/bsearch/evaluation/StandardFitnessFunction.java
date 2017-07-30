package bsearch.evaluation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.nlogolink.ModelRunningService;
import bsearch.representations.Chromosome;

public strictfp class StandardFitnessFunction implements FitnessFunction {
	private final ObjectiveFunctionInfo objectiveInfo;

	public StandardFitnessFunction(ObjectiveFunctionInfo objectiveInfo) {
		this.objectiveInfo = objectiveInfo;
	}

	public HashMap<Chromosome, Integer> getRunsNeeded(Chromosome point, int repetitionsRequested,
			ResultsArchive archive) {
		LinkedHashMap<Chromosome, Integer> map = new LinkedHashMap<Chromosome, Integer>(1);
		map.put(point, StrictMath.max(0, repetitionsRequested - archive.getResultsCount(point)));
		return map;
	}

	public int getMaximumRunsThatCouldBeNeeded(int repetitionsRequested) {
		return repetitionsRequested;
	}

	public double evaluate(Chromosome point, ResultsArchive archive, ModelRunningService runService) throws BehaviorSearchException
	{
		return (double) (runService.getCombinedResultsForEachObjective(archive.getResults( point )).get(0));
	}

	public double compare(double v1, double v2) {
		if (objectiveInfo.fitnessMinimized) {
			return v2 - v1;
		} else {
			return v1 - v2;
		}
	}

	public boolean strictlyBetterThan(double v1, double v2) {
		return compare(v1, v2) > 0.0;
	}

	public double getWorstConceivableFitnessValue() {
		return objectiveInfo.fitnessMinimized ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
	}

	public double getBestConceivableFitnessValue() {
		return objectiveInfo.fitnessMinimized ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
	}

}