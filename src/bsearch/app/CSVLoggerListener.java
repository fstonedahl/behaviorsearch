package bsearch.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import bsearch.space.SearchSpace;
import bsearch.MOEAlink.MOEASolutionWrapper;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ResultListener;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.CSVHelper;
import bsearch.nlogolink.ModelRunSetupInfo;
import bsearch.nlogolink.NLogoUtils;

public class CSVLoggerListener implements ResultListener {

	private SearchProtocolInfo protocol;
	
	private PrintWriter modelRunsOut = null;
	private PrintWriter modelRunsCheckingOut = null;
	private PrintWriter objectiveHistoryOut = null;
	private PrintWriter bestsOut = null;
	private PrintWriter finalBestsOut = null;
	private PrintWriter finalCheckedBestsOut = null;
	private PrintWriter overallBestsOut = null;
	private PrintWriter overallCheckedBestsOut = null;

	private boolean suppressNetLogoCommandCenterColumn = false;

	/**
	 * @param protocol
	 *            - the SearchProtocol being used for the search.
	 * @param fileNameStem
	 *            - for saving multiple CSV files with a common starting stem
	 * @param logAllModelRuns
	 *            - log all evaluations that occur?
	 * @param logAllObjectiveEvals
	 *            - log all fitness (objective) function evaluations that occur?
	 * @param logBests
	 *            - log the best points found?
	 * @param logFinalBests
	 *            - log the best points found?
	 * @throws IOException
	 * @throws BehaviorSearchException
	 */
	public CSVLoggerListener(SearchProtocolInfo protocol, BehaviorSearch.RunOptions runOptions)
			throws IOException, BehaviorSearchException {
		
		this.protocol = protocol;
		this.suppressNetLogoCommandCenterColumn = runOptions.suppressNetLogoCommandCenterColumn;
		String fileNameStem = runOptions.outputStem;
		if (!runOptions.suppressModelRunHistory) {
			modelRunsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".modelRuns.csv")));
			if (protocol.modelDCInfo.useBestChecking()) {
				modelRunsCheckingOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".modelRunsChecking.csv")));
			}
		}
		if (!runOptions.suppressObjectiveFunctionHistory) {
			objectiveHistoryOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".objectiveHistory.csv")));
		}
		if (!runOptions.suppressBestHistory) {
			bestsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".bestHistory.csv")));
		}
		if (!runOptions.suppressSearchBests) {
			finalBestsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".searchBests.csv")));
			if (protocol.modelDCInfo.useBestChecking()) {
				finalCheckedBestsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".searchCheckedBests.csv")));
			}
		}
		overallBestsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".overallBests.csv")));
		if (protocol.modelDCInfo.useBestChecking()) {
			overallCheckedBestsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".overallCheckedBests.csv")));
		}

		// Also output a copy of the protocol that was used for this search.
		// It's the same format as the .bsearch2 files, but we use the .json suffix
		// to differentiate it from the .bsearch2 file which was actually run.
		try {
			protocol.save(fileNameStem + ".searchConfig.json");
		} catch (java.io.IOException ex) {
			throw new BehaviorSearchException(
					"File I/O error attempting to create or write to file: '" + fileNameStem + ".searchConfig.json'", ex);
		}

	}

	@Override
	public void initListener(SearchSpace space, SearchProtocolInfo protocol) {
		List<String> paramNames = space.getParamSpecs().stream().map(p -> p.getParameterName()).collect(Collectors.toList());
		List<String> objectiveNames = protocol.objectives.stream().map(obj -> obj.name).collect(Collectors.toList());

		List<String> headerModelHistory = new LinkedList<>();
		headerModelHistory.add("[search-number]");
		headerModelHistory.add("[model-run-count]");
		headerModelHistory.add("[random-seed]");
		headerModelHistory.addAll(paramNames);
		headerModelHistory.add("[step]");
		headerModelHistory.addAll(protocol.modelDCInfo.singleRunCondenserReporters.keySet());

		List<String> headerFitness = new LinkedList<>();
		headerFitness.add("[search-number]");
		headerFitness.add("[model-run-count]");
		headerFitness.addAll(paramNames);
		headerFitness.addAll(objectiveNames);

		List<String> headerBest = new LinkedList<>();
		headerBest.addAll(headerFitness); // everything .objectiveFunctionHistory has

		if (protocol.modelDCInfo.useBestChecking()) {
			headerBest.addAll(objectiveNames.stream().map(name -> name + "-rechecked").collect(Collectors.toList()));
		}

		if (!suppressNetLogoCommandCenterColumn) {
			headerModelHistory.add("[NetLogo-command-center-copy-paste-shortcut]");
			headerFitness.add("[NetLogo-command-center-copy-paste-shortcut]");
			headerBest.add("[NetLogo-command-center-copy-paste-shortcut]");
		}

		if (modelRunsOut != null) {
			modelRunsOut.println(CSVHelper.headerRow(headerModelHistory));
			if (protocol.modelDCInfo.useBestChecking()) {
				headerModelHistory.add(2,"[num-rechecking-runs]");
				modelRunsCheckingOut.println(CSVHelper.headerRow(headerModelHistory));
			}
		}
		if (objectiveHistoryOut != null) {
			objectiveHistoryOut.println(bsearch.nlogolink.CSVHelper.headerRow(headerFitness));
		}
		for (PrintWriter out: Arrays.asList(bestsOut, finalBestsOut, finalCheckedBestsOut, overallBestsOut, overallCheckedBestsOut)) {
			if (out != null) {
				out.println(bsearch.nlogolink.CSVHelper.headerRow(headerBest));
			}
		}
	}

	@Override
	public void searchStarting(int searchID) {
	}

	@Override
	public void modelRunOccurred(int searchID, int modelRunCounter, int modelRunRecheckingCounter, boolean isRecheckingRun,
			SingleRunResult result) {

		if (modelRunsOut != null) {
			ModelRunSetupInfo runSetup = result.getModelRunSetupInfo();
			LinkedHashMap<String, Object> paramSettings = result.getModelRunSetupInfo().getParameterSettings(); 
			Collection<Object> condensedVals = result.getCondensedResultMap().values();

			List<Object> rowData;
			if (isRecheckingRun) {
				rowData = makeFlatList(searchID, modelRunCounter, modelRunRecheckingCounter, runSetup.getSeed(), 
					paramSettings.values(),result.getStepCount(), condensedVals); 
			} else {
				rowData = makeFlatList(searchID, modelRunCounter, runSetup.getSeed(), 
						paramSettings.values(),result.getStepCount(), condensedVals); 
			}

			String rowCSVString = bsearch.nlogolink.CSVHelper.dataRow(rowData);
			if (!suppressNetLogoCommandCenterColumn) {
				rowCSVString += "," + makeNetLogoCommandCenterCSVString(paramSettings,runSetup.getSeed());
			}
			if (isRecheckingRun) {
				modelRunsCheckingOut.println(rowCSVString);
			} else {
				modelRunsOut.println(rowCSVString);
			}
		}
	}

	@Override
	public void fitnessComputed(MOEASolutionWrapper computedSolution) {
		if (objectiveHistoryOut != null) {
			objectiveHistoryOut.println(makeRowStringForSolution(computedSolution));
		}
	}

	@Override
	public void newBestFound(MOEASolutionWrapper newBestWrapper) {
		if (bestsOut != null) {
			bestsOut.println(makeRowStringForSolution(newBestWrapper));
		}
	}

	@Override
	public void searchFinished(int searchID, List<MOEASolutionWrapper> bestsFromSearch, List<MOEASolutionWrapper> checkedBestsFromSearch) {
		if (finalBestsOut != null) {
			for (MOEASolutionWrapper solution: bestsFromSearch) {
				finalBestsOut.println(makeRowStringForSolution(solution));
			}
		}
		if (finalCheckedBestsOut != null) {
			for (MOEASolutionWrapper checkedSolution : checkedBestsFromSearch) {
				MOEASolutionWrapper solWrap = checkedSolution.getCheckingPairWrapper();
				finalCheckedBestsOut.println(makeRowStringForSolution(solWrap));
			}
		}
	}

	@Override
	public void allSearchesFinished(List<MOEASolutionWrapper> bestsFromAllSearches, List<MOEASolutionWrapper> checkedBestsFromAllSearches) {
		if (overallBestsOut != null) {
			for (MOEASolutionWrapper solution : bestsFromAllSearches) {
				overallBestsOut.println(makeRowStringForSolution(solution));
			}
		}
		if (overallCheckedBestsOut != null) {
			for (MOEASolutionWrapper checkedSolution : checkedBestsFromAllSearches) {
				MOEASolutionWrapper solWrap = checkedSolution.getCheckingPairWrapper();
				overallCheckedBestsOut.println(makeRowStringForSolution(solWrap));
			}
		}
		
		for (PrintWriter out : Arrays.asList(modelRunsOut, modelRunsCheckingOut, objectiveHistoryOut, bestsOut, finalBestsOut, finalCheckedBestsOut,
				overallBestsOut, overallCheckedBestsOut)) {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}
	
	// Provides a CSV-compatible text command one can copy/paste into NetLogo to achieve given parameter settings.
	//   (set seed parameter to null if it's not applicable)
	private String makeNetLogoCommandCenterCSVString(LinkedHashMap<String,Object> paramSettings, Integer seed) {
		String nlogoCmd = NLogoUtils.buildNetLogoCommandCenterString(paramSettings, seed);
		return "\"" + nlogoCmd.replace("\"", "\"\"") + "\""; // enclose and escape quotes for CSV format
	}

	// helper method to create rows for a solution in the format used by several output files
	private String makeRowStringForSolution(MOEASolutionWrapper solWrap) {
		LinkedHashMap<String, Object> paramSettings = solWrap.getParameterSettings();
		List<Object> rowData = makeFlatList(solWrap.getSearchID(), solWrap.getModelRunCounter(), paramSettings.values(),
				solWrap.getObjectiveValues());
		if (solWrap.getCheckingPairWrapper() != null) {
			rowData.addAll(solWrap.getCheckingPairWrapper().getObjectiveValues());
		}
		
		String rowCSVString = bsearch.nlogolink.CSVHelper.dataRow(rowData);
		if (!suppressNetLogoCommandCenterColumn) {
			rowCSVString += "," + makeNetLogoCommandCenterCSVString(paramSettings, null);			
		}
		return rowCSVString;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Object> makeFlatList(Object... elementsOrLists) {
		List<Object> dataList = new ArrayList<Object>();
		for (Object thing : elementsOrLists) {
			if (thing == null) { // don't include null things
				// empty
			} else if (thing instanceof Collection) {
				dataList.addAll((Collection) thing);
			} else {
				dataList.add(thing);
			}
		}
		return dataList;
	}

}
