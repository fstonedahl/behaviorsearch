package bsearch.nlogolink;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of running a NetLogo model for multiple replicate simulation runs
 * with the *same* parameter settings.
 */
public class MultipleRunResult {
	private List<SingleRunResult> singleRuns;
	private List<Object> combinedMeasures;
	
	public MultipleRunResult(int numRunsToStore) {
		this.singleRuns = new ArrayList<>(numRunsToStore);
	} 
	
	public void addSingleRun(SingleRunResult newRun) {
		singleRuns.add(newRun);
	}

	public List<SingleRunResult> getSingleRuns() {
		return singleRuns;
	}

	public List<Object> getCombinedMeasures() {
		return combinedMeasures;
	}

	public void setCombinedMeasures(List<Object> combinedMeasures) {
		this.combinedMeasures = combinedMeasures;
	}	
}
