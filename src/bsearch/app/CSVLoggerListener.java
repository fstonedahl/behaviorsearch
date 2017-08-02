package bsearch.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import bsearch.representations.Chromosome;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;
import bsearch.datamodel.ObjectiveFunctionInfo;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchProgressStatsKeeper;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.CSVHelper;
import bsearch.nlogolink.ModelRunSetupInfo;
import bsearch.nlogolink.NLogoUtils;

public class CSVLoggerListener implements ResultListener {
	
	private SearchProtocolInfo protocol;
	private PrintWriter modelHistoryOut = null;
	private PrintWriter fitnessOut = null;
	private PrintWriter bestOut = null;
	private PrintWriter finalBestOut = null;
	private PrintWriter finalCheckedBestOut = null;
	
	
	/**
	 * @param protocol - the SearchProtocol being used for the search.
	 * @param fileNameStem - for saving multiple CSV files with a common starting stem
	 * @param logAllModelRuns - log all evaluations that occur? 
	 * @param logAllFitnessEvals - log all fitness (objective) function evaluations that occur?
	 * @param logBests - log the best points found?
	 * @param logFinalBests - log the best points found?
	 * @throws IOException
	 * @throws BehaviorSearchException 
	 */
	public CSVLoggerListener(SearchProtocolInfo protocol, String fileNameStem, boolean logAllModelRuns, boolean logAllFitnessEvals, boolean logBests, boolean logFinalBests) throws IOException, BehaviorSearchException 
	{
		this.protocol = protocol;		
		if (logAllModelRuns)
		{
			modelHistoryOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".modelRunHistory.csv")));
		}
		if (logAllFitnessEvals)
		{
			fitnessOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".objectiveFunctionHistory.csv")));			
		}
		if (logBests)
		{
			bestOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".bestHistory.csv")));
		}
		if (logFinalBests)
		{
			finalBestOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".finalBests.csv")));
			if (protocol.searchAlgorithmInfo.useBestChecking())
			{
				finalCheckedBestOut = new PrintWriter(new BufferedWriter(new FileWriter(fileNameStem + ".finalCheckedBests.csv")));	
			}
		}
		
		// Also output a copy of the protocol that was used for this search.  
		// It's the same format as the .bsearch2 files, but we use the .json suffix
		// to differentiate it from the .bsearch2 file which was actually run.
		try { 
			protocol.save(fileNameStem + ".searchConfig.json");
		} catch (java.io.IOException ex)
		{
			throw new BehaviorSearchException("File I/O error attempting to create or write to file: '" + fileNameStem + ".searchConfig.json'", ex);
		}

	}

	@Override
	public void initListener(SearchSpace space, SearchProtocolInfo protocol)
	{
		List<String> paramNames = new LinkedList<String>();
		for (ParameterSpec p : space.getParamSpecs())
		{
			paramNames.add(p.getParameterName());
		}

		if (modelHistoryOut != null)
		{
			List<String> headerList = new LinkedList<>();
			headerList.add("[search-number]");
			headerList.add("[evaluation-number]");
			headerList.add("[random-seed]");
			headerList.addAll(paramNames);
			headerList.add("[step]");
			for (String name : protocol.modelDCInfo.singleRunCondenserReporters.keySet()) {
				headerList.add(name);
			}
			modelHistoryOut.println(CSVHelper.headerRow(headerList));
		}

		List<String> headerCommon = new LinkedList<>();
		headerCommon.add("[search-number]");
		headerCommon.add("[evaluation-number]");
		headerCommon.addAll(paramNames);

		for (ObjectiveFunctionInfo objInfo : protocol.objectives) {
			headerCommon.add(objInfo.name);
		}

		if (fitnessOut != null)
		{
			fitnessOut.println(bsearch.nlogolink.CSVHelper.headerRow(headerCommon));					
		}
		
		if (protocol.searchAlgorithmInfo.useBestChecking())
		{
			for (ObjectiveFunctionInfo objInfo : protocol.objectives) {
				headerCommon.add(objInfo.name + "-rechecked");
			}
		}
		if (bestOut != null)
		{
			bestOut.println(bsearch.nlogolink.CSVHelper.headerRow(headerCommon));
		}
		headerCommon.add("[NetLogo-command-center-copy-paste-shortcut]");
		if (finalBestOut != null)
		{
			finalBestOut.println(bsearch.nlogolink.CSVHelper.headerRow(headerCommon));
		}
		if (finalCheckedBestOut != null)
		{
			finalCheckedBestOut.println(bsearch.nlogolink.CSVHelper.headerRow(headerCommon));
		}
	}

	@Override
	public void modelRunOccurred(int searchID, int modelRunCounter, int modelRunRecheckingCounter, SingleRunResult result) 
	{
		if (modelHistoryOut == null)
		{
			return;
		}
		ModelRunSetupInfo runSetup = result.getModelRunSetupInfo();

		List<Object> dataList = new LinkedList<Object>();
		dataList.add(searchID);
		dataList.add(modelRunCounter);
		dataList.add(runSetup.getSeed()); 
		
		for (Object paramVal : result.getModelRunSetupInfo().getParameterSettings().values()) {
			dataList.add(paramVal);
		}
		dataList.add(result.getStepCount()); 
		
		for (Object condensedVal: result.getCondensedResultMap().values()) {
			dataList.add(condensedVal);
		}
		modelHistoryOut.println(bsearch.nlogolink.CSVHelper.dataRow(dataList));
	}

	@Override
	public void fitnessComputed(SearchProgressStatsKeeper statsKeeper, LinkedHashMap<String,Object> paramSettings, 
			double[] objectiveVals)
	{
		if (fitnessOut == null)
		{
			return;
		}
		List<Object> dataList = new LinkedList<Object>();
		dataList.add(statsKeeper.getSearchIDNumber());
		dataList.add(statsKeeper.getModelRunCounter());
		for (Object paramVal : paramSettings.values())
		{
			dataList.add(paramVal);
		}
		for (double objVal : objectiveVals)
		{
			dataList.add(objVal);
		}
		
		fitnessOut.println(bsearch.nlogolink.CSVHelper.dataRow(dataList));
	}

	private List<Object> makeRowDataFor(SearchProgressStatsKeeper statsKeeper, Chromosome point, double[] objValues, double[] objValuesRechecked)
	{
		List<Object> dataList = new ArrayList<Object>();
		dataList.add(statsKeeper.getSearchIDNumber());
		dataList.add(statsKeeper.getModelRunCounter());
		
		for (ParameterSpec p : point.getSearchSpace().getParamSpecs())
		{
			dataList.add(point.getParamSettings().get(p.getParameterName()));
		}
		for (double d: objValues) {
			dataList.add(d);
		}
		if (protocol.searchAlgorithmInfo.useBestChecking())
		{			
			for (double d: objValuesRechecked) {
				dataList.add(d);
			}
		}
		return dataList;		
	}
	
	
	@Override
	public void newBestFound(SearchProgressStatsKeeper statsKeeper)
	{
		List<Object> rowData = makeRowDataFor(statsKeeper, statsKeeper.getCurrentBest(), statsKeeper.reportCurrentBestFitness(),
													statsKeeper.reportCurrentBestCheckedFitness());
		
		if (bestOut != null)
		{
			bestOut.println(bsearch.nlogolink.CSVHelper.dataRow(rowData));	
		}
	}
	@Override
	public void searchStarting(SearchProgressStatsKeeper statsKeeper)
	{
	}
	@Override
	public void searchFinished(SearchProgressStatsKeeper statsKeeper)
	{
		if (finalBestOut != null)
		{
			List<Object> rowData = makeRowDataFor(statsKeeper, statsKeeper.getCurrentBest(), statsKeeper.reportCurrentBestFitness(),
													statsKeeper.reportCurrentBestCheckedFitness());
			String nlogoCmd = NLogoUtils.buildNetLogoCommandCenterString(statsKeeper.getCurrentBest().getParamSettings());
			nlogoCmd.replace("\"", "\"\"");
			finalBestOut.println(bsearch.nlogolink.CSVHelper.dataRow(rowData)+",\""+nlogoCmd+"\"");
		}
		if (finalCheckedBestOut != null)
		{
			List<Object> rowData = makeRowDataFor(statsKeeper, statsKeeper.getCurrentCheckedBest(), 
										statsKeeper.reportCurrentCheckedBestFitness(), 
										statsKeeper.reportCurrentCheckedBestCheckedFitness());
			rowData.add(NLogoUtils.buildNetLogoCommandCenterString(statsKeeper.getCurrentCheckedBest().getParamSettings()));
			finalCheckedBestOut.println(bsearch.nlogolink.CSVHelper.dataRow(rowData));
		}
		
	}

	@Override
	public void allSearchesFinished() {
		if (modelHistoryOut != null)
		{
			modelHistoryOut.flush();
			modelHistoryOut.close();
		}
		if (fitnessOut != null)
		{
			fitnessOut.flush();
			fitnessOut.close();
		}
		if (bestOut != null)
		{
			bestOut.flush();
			bestOut.close();
		}
		if (finalBestOut != null)
		{
			finalBestOut.flush();
			finalBestOut.close();
		}
		if (finalCheckedBestOut != null)
		{
			finalCheckedBestOut.flush();
			finalCheckedBestOut.close();
		}
	}

	@Override
	public void searchesAborted() {
		// for now, handle aborted runs the same way as if it completed successfully.
		allSearchesFinished();
	}

}
