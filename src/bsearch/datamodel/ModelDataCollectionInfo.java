package bsearch.datamodel;

import java.util.LinkedHashMap;

public class ModelDataCollectionInfo {

	public static final String SPECIAL_MEASURE_IF_DONE_FLAG = "@DONE";
	
	public final String modelFileName, setupCommands, stepCommands, stopCondition, measureIfReporter;
	public final int maxModelSteps;		
	public final LinkedHashMap<String,String> measureReporters;
	public final LinkedHashMap<String,String> singleRunCondenserReporters;
	
	@SuppressWarnings("unused") // used to set future default values for new fields (only used by Gson)
	public ModelDataCollectionInfo()
	{
		this.modelFileName = null;
		this.maxModelSteps = 0;
		this.setupCommands = null;
		this.stepCommands = null;
		this.stopCondition = null;
		this.measureIfReporter = null;
		this.measureReporters = null;
		this.singleRunCondenserReporters = null;
	}

	public ModelDataCollectionInfo(String modelFileName,  int maxModelSteps, 
			String setupCommands, String stepCommands, String stopCondition, String measureIfReporter,
			LinkedHashMap<String,String> measureReporters, LinkedHashMap<String,String> singleRunCondenserReporters)
	{
		this.modelFileName = modelFileName;
		this.maxModelSteps = maxModelSteps;

		this.setupCommands = setupCommands;
		this.stepCommands = stepCommands;
		this.stopCondition = stopCondition;
		if (measureIfReporter.equalsIgnoreCase(SPECIAL_MEASURE_IF_DONE_FLAG)) { 
			measureIfReporter = SPECIAL_MEASURE_IF_DONE_FLAG; // convert to upper case here for more efficient flag checking later
		}
		this.measureIfReporter = measureIfReporter;
		this.measureReporters = measureReporters;
		this.singleRunCondenserReporters = singleRunCondenserReporters;
	}

}
