package bsearch.nlogolink;

import java.util.LinkedHashMap;

import org.nlogo.core.LogoList;
/**
 * Class for storing results from a model run.  It can store multiple measures. 
 */
public class ModelRunResult {
	private final ModelRunSetupInfo runSetup;
	private final LinkedHashMap<String,Object> condensedResultMap; 
	private final LinkedHashMap<String,LogoList> rawResults; 
	
	public ModelRunResult(ModelRunSetupInfo runSetup, LinkedHashMap<String,LogoList> fullResults, LinkedHashMap<String,Object> condensedResults)
	{
		this.runSetup = runSetup;
		this.rawResults = fullResults;
		this.condensedResultMap = condensedResults;
	}
	public String[] getRawMeasureNames()
	{
		return rawResults.keySet().toArray(new String[0]);
	}
	public LogoList getRawMeasureData(String measure)
	{
		return rawResults.get(measure);
	}
	public LinkedHashMap<String,Object> getCondensedResultMap() {
		return condensedResultMap;
	}
	/*public String[] getCondensedMeasureNames()
	{
		return condensedResultMap.keySet().toArray(new String[0]);
	}
	public Object getCondensedMeasureData(String measure)
	{
		return condensedResultMap.get(measure);
	}
	public Object getPrimaryCondensedResult()
	{
		return getCondensedMeasureData(getCondensedMeasureNames()[0]);
	}*/
	
	public ModelRunSetupInfo getModelRunSetupInfo()
	{
		return runSetup;
	}
	
}
