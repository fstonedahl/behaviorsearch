package bsearch.nlogolink;

import java.util.LinkedHashMap;

import org.nlogo.core.LogoList;
/**
 * Class for storing results from a model run.  It can store multiple measures. 
 */
public class SingleRunResult {
	private final ModelRunSetupInfo runSetup;
	private final LinkedHashMap<String,Object> condensedResultMap; 
	private final LinkedHashMap<String,LogoList> rawResults; 
	private final int stepCount;
	
	public SingleRunResult(ModelRunSetupInfo runSetup, LinkedHashMap<String,LogoList> fullResults, 
			LinkedHashMap<String,Object> condensedResults, int stepCount)
	{
		this.runSetup = runSetup;
		this.rawResults = fullResults;
		this.condensedResultMap = condensedResults;
		this.stepCount = stepCount;
	}
	public String[] getRawMeasureNames()
	{
		if (rawResults == null) {
			throw new IllegalStateException("raw model run results weren't stored (presumably to save space)");
		}
		return rawResults.keySet().toArray(new String[0]);
	}
	public LogoList getRawMeasureData(String measure)
	{
		if (rawResults == null) {
			throw new IllegalStateException("raw model run results weren't stored (presumably to save space)");
		}
		return rawResults.get(measure);
	}
	public LinkedHashMap<String,Object> getCondensedResultMap() {
		return condensedResultMap;
	}
	
	public ModelRunSetupInfo getModelRunSetupInfo()
	{
		return runSetup;
	}
	public int getStepCount() {
		return stepCount;
	}
	
}
