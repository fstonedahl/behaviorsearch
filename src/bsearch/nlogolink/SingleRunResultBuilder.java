package bsearch.nlogolink;

import java.util.LinkedHashMap;
import org.nlogo.api.JobOwner;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.core.LogoList;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Procedure;
/**
 * Class for building up results from a model run.  
 */
public class SingleRunResultBuilder {
	private LinkedHashMap<String,LogoListBuilder> measureResultsOverTimeBuilders = new LinkedHashMap<>(); 
	
	public SingleRunResultBuilder()
	{
	}
	
	public void appendMeasureResult(String measure, Object val) // TODO: add stepNum
	{
		if (!measureResultsOverTimeBuilders.containsKey(measure))
		{
			measureResultsOverTimeBuilders.put(measure, new LogoListBuilder());
		}
		measureResultsOverTimeBuilders.get(measure).add(val);
	}
	
	/**
	 * 
	 * @return a mapping of measure names to the LogoList of collected results for each measure 
	 */
	private LinkedHashMap<String,LogoList> getMeasureResults() {
		LinkedHashMap<String, LogoList> measureResults = new LinkedHashMap<>();
		for (String key : measureResultsOverTimeBuilders.keySet()) {
				measureResults.put(key, measureResultsOverTimeBuilders.get(key).toLogoList());
		}
		return measureResults;
	}
	
	public SingleRunResult createModelRunResults(ModelRunSetupInfo runSetup,
			String[] originalCondenserCodes,
			LinkedHashMap<String,Procedure> singleRunCondensers, 
			HeadlessWorkspace workspace, JobOwner owner, int stepCount,
			boolean includeRawResults) throws NetLogoLinkException {
		
		LinkedHashMap<String, LogoList> rawResults = getMeasureResults();

		String[] varNames = rawResults.keySet().toArray(new String[0]);
		Object[] varValues = rawResults.values().toArray();

		String[] condenserNames = singleRunCondensers.keySet().toArray(new String[0]);
		LinkedHashMap<String, Object> condensedResults = new LinkedHashMap<>();
		
		for (int i = 0; i < condenserNames.length; i++) {
			Object condensedResult = NLogoUtils.evaluateNetLogoWithSubstitution(originalCondenserCodes[i], singleRunCondensers.get(condenserNames[i]), 
					varNames, varValues, workspace, owner);
			condensedResults.put(condenserNames[i], condensedResult);
		}
		if (includeRawResults) {
			return new SingleRunResult(runSetup, rawResults, condensedResults, stepCount);
		} else {
			return new SingleRunResult(runSetup, null, condensedResults, stepCount);
		}
		
	}
	


}
