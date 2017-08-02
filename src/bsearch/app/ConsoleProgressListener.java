package bsearch.app;

import java.io.PrintStream;
import java.util.LinkedHashMap;

import bsearch.representations.Chromosome;
import bsearch.space.SearchSpace;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.evaluation.SearchProgressStatsKeeper;
import bsearch.nlogolink.SingleRunResult;

public class ConsoleProgressListener implements ResultListener {
	private String lastProgress;
	private int totalEvals;
	private PrintStream out;
	
	public ConsoleProgressListener(int totalEvals, PrintStream out) 
	{
		this.totalEvals = totalEvals;
		this.out = out;
	}

	@Override
	public void modelRunOccurred(int searchID, int modelRunCounter, int modelRunRecheckingCounter, SingleRunResult result) 
	{
		String progress = String.format("Search %s: %.0f%%\n", searchID,
				((double) modelRunCounter + modelRunRecheckingCounter) / (totalEvals + modelRunRecheckingCounter ) * 100);
		if (!progress.equals(lastProgress))
		{
			out.print(progress);
			lastProgress = progress;
		}
	}

	@Override
	public void fitnessComputed(SearchProgressStatsKeeper statsKeeper, LinkedHashMap<String, Object> paramSettings,
			double[] fitness) {
		
	}

	@Override
	public void newBestFound(SearchProgressStatsKeeper statsKeeper)
	{
	}

	@Override
	public void searchStarting(SearchProgressStatsKeeper statsKeeper)
	{
		lastProgress = "N/A";
	}
	@Override
	public void searchFinished(SearchProgressStatsKeeper statsKeeper)
	{
	}

	@Override
	public void initListener(SearchSpace space, SearchProtocolInfo protocol) {
	}
	
	@Override
	public void allSearchesFinished() {
		out.printf("All searches completed.\n\n");
	}
	
	@Override
	public void searchesAborted() {
		out.printf("Searches were aborted.\n\n");
	}


}
