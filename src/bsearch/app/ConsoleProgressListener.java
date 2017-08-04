package bsearch.app;

import java.io.PrintStream;
import java.util.List;

import bsearch.space.SearchSpace;
import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ResultListener;
import bsearch.nlogolink.SingleRunResult;

public class ConsoleProgressListener implements ResultListener {
	private String lastProgress;
	private int totalEvals;
	private PrintStream out;

	public ConsoleProgressListener(int totalEvals, PrintStream out) {
		this.totalEvals = totalEvals;
		this.out = out;
	}

	@Override
	public void initListener(SearchSpace space, SearchProtocolInfo protocol) {
	}

	@Override
	public void searchStarting(int searchID) {
		lastProgress = "N/A";
	}

	@Override
	public void modelRunOccurred(int searchID, int modelRunCounter, int modelRunRecheckingCounter, boolean isRecheckingRun,
			SingleRunResult result) {

		double runsSoFar = modelRunCounter + modelRunRecheckingCounter;
		double estimatedTotalRuns = totalEvals + modelRunRecheckingCounter;
		double percentProgress = runsSoFar / estimatedTotalRuns * 100;
		String progress = String.format("Search %s: %.0f%%\n", searchID, percentProgress);
		if (!progress.equals(lastProgress)) {
			out.print(progress);
			lastProgress = progress;
		}
	}

	@Override
	public void fitnessComputed(MOEASolutionWrapper computeWrapper) {
	}

	@Override
	public void newBestFound(MOEASolutionWrapper newBestWrapper) {
	}

	@Override
	public void searchFinished(int searchID, List<MOEASolutionWrapper> bestsFromSearch, List<MOEASolutionWrapper> checkedBestsFromSearch) {
	}

	@Override
	public void allSearchesFinished(List<MOEASolutionWrapper> bestsFromAllSearches, List<MOEASolutionWrapper> checkedBestsFromAllSearches) {

		out.printf("All searches completed.\n\n");
	}

}
