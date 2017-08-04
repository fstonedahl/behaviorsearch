package bsearch.datamodel;

import java.util.LinkedHashMap;

public class ModelDataCollectionInfo {

	public static final String SPECIAL_MEASURE_IF_DONE_FLAG = "@DONE";
	
	public final String modelFileName, setupCommands, stepCommands, stopCondition, measureIfReporter;
	public final int maxModelSteps;		
	public final LinkedHashMap<String,String> measureReporters;
	public final LinkedHashMap<String,String> singleRunCondenserReporters;
	public final int fitnessSamplingReplications ;
	public final int bestCheckingNumReplications; // if == 0, no best checking is done		
	
	@SuppressWarnings("unused") // used to set future default values for new fields (only used by Gson)
	private ModelDataCollectionInfo()
	{
		this.modelFileName = null;
		this.maxModelSteps = 0;
		this.setupCommands = null;
		this.stepCommands = null;
		this.stopCondition = null;
		this.measureIfReporter = null;
		this.measureReporters = null;
		this.singleRunCondenserReporters = null;
		this.fitnessSamplingReplications = 0;
		this.bestCheckingNumReplications = 0;		
	}

	public ModelDataCollectionInfo(String modelFileName,  int maxModelSteps, 
			String setupCommands, String stepCommands, String stopCondition, String measureIfReporter,
			LinkedHashMap<String,String> measureReporters, LinkedHashMap<String,String> singleRunCondenserReporters,
			int fitnessSamplingReplications, int bestCheckingNumReplications)
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
		this.fitnessSamplingReplications = fitnessSamplingReplications;
		this.bestCheckingNumReplications = bestCheckingNumReplications;
	}

	public boolean useBestChecking() {
		return bestCheckingNumReplications > 0;
	}

}
