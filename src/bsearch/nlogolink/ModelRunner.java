package bsearch.nlogolink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.nlogo.api.AgentException;
import org.nlogo.core.CompilerException;
import org.nlogo.core.LogoList;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.SimpleJobOwner;
import org.nlogo.nvm.Procedure;

import bsearch.datamodel.ModelDataCollectionInfo;
import bsearch.util.GeneralUtils;

import org.nlogo.api.MersenneTwisterFast;
import org.nlogo.headless.HeadlessWorkspace;

public strictfp class ModelRunner {

	private HeadlessWorkspace workspace;

	private ModelDataCollectionInfo modelDCInfo;

	// use pre-compiled versions of all NetLogo code for efficiency
	private Procedure setupCommandsProcedure;
	private Procedure stepCommandsProcedure;
	private Procedure stopConditionReporterProcedure;
	private Procedure measureIfReporterProcedure = null;
	
	// Note: ModelRunner can collect multiple measures
	private LinkedHashMap<String,Procedure> measureReporterProcs = new LinkedHashMap<String,Procedure>();

	private LinkedHashMap<String,Procedure> singleRunCondenserReporterProcs = new LinkedHashMap<String,Procedure>();

	// Admittedly, combining doesn't pertain to any single model run, but we need access
	// to the model/workspace in order to compile and execute NetLogo code,
	//  AND the code must be compiled in the same world that it gets executed in!
	private List<String> multipleRunCombinerSourceCodes;
	private List<Procedure> multipleRunCombinerProcs = new ArrayList<>();

	private boolean runIsDone; 

	// Until setup() is run for the first time (and the random seed can be set appropriately), 
	// we'll leave the JobOwners as null.  
	private SimpleJobOwner mainJobOwner = null; // uses NetLogo world's mainRNG (and thus affects the state of the world)
	private SimpleJobOwner extraJobOwner = null; // uses a private RNG, and thus may not affect world state (unless the NetLogo code being run has side effects)
	
	/**
	 * Should probably not be called directly - instead allow the Pool/Factory to create it
	 * @throws NetLogoLinkException 
	 */
	ModelRunner(ModelDataCollectionInfo modelDCInfo, List<String> combineReporterSourceCodes) throws NetLogoLinkException  
	{
		this.modelDCInfo = modelDCInfo;
		this.multipleRunCombinerSourceCodes = combineReporterSourceCodes;

		workspace = NLogoUtils.createWorkspace();
    	try {    		
    		workspace.open(GeneralUtils.attemptResolvePathFromProtocolFolder(modelDCInfo.modelFileName));			
    	} catch (LogoException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("Error opening NetLogo model.  NetLogo sent back this error message: \"" + e.getMessage() + "\"");
		} catch (IOException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("I/O error when loading NetLogo model ( " + modelDCInfo.modelFileName + " ).  Error message: \"" + e.getMessage() + "\"");
		} catch (CompilerException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("Error compiling NetLogo model ( " + modelDCInfo.modelFileName + " ).  NetLogo's error message: \"" + e.getMessage() + "\"");
		}
    	
		setSetupCommands(modelDCInfo.setupCommands);		
		setStepCommands(modelDCInfo.stepCommands);
		setStopConditionReporter(modelDCInfo.stopCondition );
		setMeasureIfReporter(modelDCInfo.measureIfReporter);
		
		for (String reporterName : modelDCInfo.measureReporters.keySet()) {
			addMeasureReporter(reporterName, modelDCInfo.measureReporters.get(reporterName));
		}
		// 
		String[] measureVarNames = modelDCInfo.measureReporters.keySet().toArray(new String[0]);
		for (String reporterName : modelDCInfo.singleRunCondenserReporters.keySet()) {
			addSingleRunCondenserReporter(reporterName, modelDCInfo.singleRunCondenserReporters.get(reporterName), measureVarNames);
		}

		String[] condenserVarNames = modelDCInfo.singleRunCondenserReporters.keySet().toArray(new String[0]);
		for (String reporterSource : multipleRunCombinerSourceCodes) {
			addMultipleRunCombinerReporter(reporterSource, condenserVarNames);
		}
	}

	public void setSetupCommands(String commands) throws NetLogoLinkException
	{
		try {
			setupCommandsProcedure = workspace.compileCommands(commands);
		} catch (CompilerException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("Error compiling the model's setup commands : " +commands.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
		}
	}
	public void setStepCommands(String commands) throws NetLogoLinkException
	{
		try {
			stepCommandsProcedure = workspace.compileCommands(commands);
		} catch (CompilerException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("Error compiling the model's step commands : " +commands.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
		}
			
	}
	public void setStopConditionReporter(String reporter) throws NetLogoLinkException
	{
		if (reporter.trim().length() > 0)
		{
			try {
				stopConditionReporterProcedure = workspace.compileReporter(reporter);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's stop condition reporter: " + reporter.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}

		}
	}	
	public void setMeasureIfReporter(String reporter) throws NetLogoLinkException
	{
		if (reporter.trim().length() > 0 && !reporter.equals(ModelDataCollectionInfo.SPECIAL_MEASURE_IF_DONE_FLAG))
		{
			try {
				measureIfReporterProcedure = workspace.compileReporter(reporter);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's 'measure if' reporter: " + reporter.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
		}
	}
	public void addMeasureReporter(String reporterName, String reporter) throws NetLogoLinkException
	{
		try {
			measureReporterProcs.put(reporterName, workspace.compileReporter(reporter));
		} catch (CompilerException e) {
			e.printStackTrace();
			throw new NetLogoLinkException("Error compiling model's measure reporter: " + reporter.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
		}
	}

	public void addSingleRunCondenserReporter(String reporterName, String reporter, String[] measureVarNames) throws NetLogoLinkException
	{
		singleRunCondenserReporterProcs.put(reporterName, NLogoUtils.substituteVariablesAndCompile(reporter, measureVarNames, workspace));
	}

	public void addMultipleRunCombinerReporter(String reporter, String[] condensedVarNames) throws NetLogoLinkException
	{
		multipleRunCombinerProcs.add(NLogoUtils.substituteVariablesAndCompile(reporter, condensedVarNames, workspace));
	}

	
	public boolean checkStopCondition() throws NetLogoLinkException
	{
		if (extraJobOwner == null)
		{
			throw new IllegalStateException("ModelRunner.setup() must be called before running commands/reporters.");
		}
		if (stopConditionReporterProcedure != null)
		{
			Object obj = workspace.runCompiledReporter( extraJobOwner, stopConditionReporterProcedure);
			LogoException ex = workspace.lastLogoException();
			if (ex != null)
			{
				workspace.lastLogoException_$eq(null);
				throw new NetLogoLinkException(ex.toString());
			}
			if (!(obj instanceof Boolean))
			{
				throw new NetLogoLinkException("The stop condition reporter must report a TRUE/FALSE value.  Error occurred because it reported: " + obj);
			}
			return 	(Boolean) obj;
		}
		return false;
	}
	
	public void setup(int seed, LinkedHashMap<String,Object> parameterSettings ) throws LogoException, AgentException, NetLogoLinkException
	{
		workspace.clearAll();
		for (String s: parameterSettings.keySet())
		{
			workspace.world().setObserverVariableByName(s, parameterSettings.get(s));
		}
		try {
			workspace.world().mainRNG().setSeed( seed );
			// For evaluating reporters (like measureIfReporter) we want to use a separate RNG 
			// (which is seeded by a random number that depends on the random seed for this model run,
			//  so that the results are deterministic/repeatable, but will generate an independent 
			// stream of random numbers that does not affect the main NetLogo world's RNG.)
			// This approach should allow the user to recreate a run by setting RANDOM-SEED X
			// and running SETUP followed by GO, without worrying about all of the additional conditions
			// and reporters affecting the state of the RNG and changing the outcome of the run...
			mainJobOwner = new SimpleJobOwner("BehaviorSearch ModelRunner Main", workspace.mainRNG(), org.nlogo.core.AgentKindJ.Observer());
			
			MersenneTwisterFast extraReporterRNG = new MersenneTwisterFast(workspace.mainRNG().clone().nextInt());
			extraJobOwner = new SimpleJobOwner("BehaviorSearch ModelRunner Extra", extraReporterRNG, org.nlogo.core.AgentKindJ.Observer());
		} catch (Exception ex) {ex.printStackTrace(); }
		
		if (setupCommandsProcedure != null)
		{
			workspace.runCompiledCommands(mainJobOwner,setupCommandsProcedure);
			LogoException ex = workspace.lastLogoException();
			if (ex != null)
			{
				workspace.lastLogoException_$eq(null);
				throw new NetLogoLinkException(ex.toString());
			}
		}
		
		runIsDone = checkStopCondition();		
	}
	
	/** returns true if the run is finished 
	 * @throws NetLogoLinkException **/
	public boolean go() throws NetLogoLinkException
	{
		if (mainJobOwner == null)
		{
			throw new IllegalStateException("ModelRunner.setup() must be called before running commands/reporters.");
		}
		if (stepCommandsProcedure != null)
		{
			workspace.runCompiledCommands(mainJobOwner, stepCommandsProcedure );				
			LogoException ex = workspace.lastLogoException();
			if (ex != null)
			{
				workspace.lastLogoException_$eq(null);
				throw new NetLogoLinkException(ex.toString());
			}
		}

		runIsDone = checkStopCondition();
		return runIsDone;
	}
	
	public LinkedHashMap<String,Object> measureResults() throws NetLogoLinkException
	{
		LinkedHashMap<String,Object> results = new LinkedHashMap<String,Object>();
		for (String key: measureReporterProcs.keySet())
		{
			results.put(key, measureResultReporter(measureReporterProcs.get(key)));
		}		
		return results;
	}
	
	private Double measureResultReporter(Procedure reporter) throws NetLogoLinkException
	{
		if (extraJobOwner == null)
		{
			throw new IllegalStateException("ModelRunner.setup() must be called before running commands/reporters.");
		}
		Object obj = workspace.runCompiledReporter( extraJobOwner, reporter);
		LogoException ex = workspace.lastLogoException();
		if (ex != null)
		{
			workspace.lastLogoException_$eq(null);
			throw new NetLogoLinkException(ex.toString());
		}
		if (! (obj instanceof Double))
		{
			throw new NetLogoLinkException("Result reporters must return numeric results!  Invalid reported value was: " + obj );				
		}
		return (Double) obj;
	}

	private boolean evaluateMeasureIfReporter(boolean isAfterFinalStep) throws NetLogoLinkException
	{
		if (measureIfReporterProcedure == null) 
		{
			if (modelDCInfo.measureIfReporter.equals(ModelDataCollectionInfo.SPECIAL_MEASURE_IF_DONE_FLAG)) {
				return isAfterFinalStep;
			} else {// they left the field blank, and so we assume we should measure every time.
				return true;				
			}
		}
		Object obj = workspace.runCompiledReporter(extraJobOwner, measureIfReporterProcedure).equals(Boolean.TRUE);
		LogoException ex = workspace.lastLogoException();
		if (ex != null)
		{
			workspace.lastLogoException_$eq(null);
			throw new NetLogoLinkException(ex.toString());
		}
		if (!(obj instanceof Boolean))
		{
			throw new NetLogoLinkException("The 'measure if' condition must report a TRUE/FALSE value.  Error occurred because it reported: " + obj);
		}
		return 	(Boolean) obj;		
	}
	private void conditionallyRecordResults(SingleRunResultBuilder resultBuilder, boolean isAfterFinalStep) throws NetLogoLinkException
	{
		if (extraJobOwner == null)
		{
			throw new IllegalStateException("ModelRunner.setup() must be called before running commands/reporters.");
		}

		if (evaluateMeasureIfReporter(isAfterFinalStep))
		{
			for (String measureName: measureReporterProcs.keySet())
			{
				resultBuilder.appendMeasureResult(measureName, measureResultReporter(measureReporterProcs.get(measureName)));
			}
		}		
	}
		
	public SingleRunResult doFullRun(ModelRunSetupInfo runSetup) throws ModelRunnerException
	{
		try {
			SingleRunResultBuilder resultBuilder = new SingleRunResultBuilder();
			setup(runSetup.getSeed(), runSetup.getParameterSettings());
			
			int steps;
			for (steps = 0; steps < modelDCInfo.maxModelSteps && !runIsDone; steps++)
			{
				conditionallyRecordResults(resultBuilder,false);
				go();
			}
			conditionallyRecordResults(resultBuilder,true);
			String[] originalCondenserCodes = modelDCInfo.singleRunCondenserReporters.values().toArray(new String[0]);

			return resultBuilder.createModelRunResults(runSetup, originalCondenserCodes, 
					singleRunCondenserReporterProcs, workspace, extraJobOwner, steps);
		}
		catch (Exception ex)  {		
			throw new ModelRunnerException(runSetup, ex);
		}
	}
	
	public boolean isRunDone()
	{
		return runIsDone;
	}
	
	// After multiple runs are done, then this method will get called on (an arbitrary) ModelRunner
	// to combine the results across runs.
	public List<Object> evaluateCombineReplicateReporters(List<SingleRunResult> replicateRunsResults)
			throws NetLogoLinkException {
		String[] condensedVarNames =  modelDCInfo.singleRunCondenserReporters.keySet().toArray(new String[0]);
		
		LogoList[] condensedVarValues= new LogoList[condensedVarNames.length];

		for (int i = 0; i < condensedVarNames.length; i++) { 
			LogoListBuilder builder = new LogoListBuilder();
			for (SingleRunResult runResult : replicateRunsResults) {
				builder.add(runResult.getCondensedResultMap().get(condensedVarNames[i]));
			}
			condensedVarValues[i] = builder.toLogoList();
		}
		
		SimpleJobOwner tempJobOwner = new SimpleJobOwner("BehaviorSearch ModelRunner Combiner", workspace.mainRNG().clone(), org.nlogo.core.AgentKindJ.Observer());
		List<Object> combinedResults = new ArrayList<Object>();
		for (int i = 0; i < multipleRunCombinerProcs.size(); i++) {
			combinedResults.add(NLogoUtils.evaluateNetLogoWithSubstitution(multipleRunCombinerSourceCodes.get(i),
					multipleRunCombinerProcs.get(i), condensedVarNames, condensedVarValues, workspace, tempJobOwner));
		}
		return combinedResults;
	}

	
	public void dispose() throws InterruptedException {
		workspace.dispose();		
	}
		

	/** Note: This uses the "extra" random number generator, so it 
	 * won't affect the state of the main RNG for the run.  
	 * Still, be careful because some reporters could have side-effects in the model
	 * (for instance, if the reporter creates some agents 
	 * (even if it kills them before its done, the "who" numbers of agents 
	 *  created after that will be different...) 
	 * @param reporter	the NetLogo reporter expression to run.
	 * @return 			the result of running the reporter
	 * @throws CompilerException
	 * @throws NetLogoLinkException 
	 */
	public Object report(String reporter) throws CompilerException, NetLogoLinkException
	{
		Procedure procReporter = workspace.compileReporter( reporter );
		Object obj = workspace.runCompiledReporter( extraJobOwner, procReporter );
		LogoException ex = workspace.lastLogoException();
		if (ex != null)
		{
			workspace.lastLogoException_$eq(null);
			throw new NetLogoLinkException(ex.toString());
		}
		return obj;
	}
	/**
	 * Note: this method uses the main random-number generator, and thus 
	 * will affect the state of the RNG for future calls.
	 * @param cmd - the NetLogo command to be run on the current headless workspace.
	 * @throws CompilerException
	 * @throws LogoException
	 */
	public void command(String cmd) throws CompilerException, LogoException
	{
		workspace.command( cmd );
	}

	
	
	/** Just used for unit testing - preferable to acquire from the Pool */
	public static ModelRunner createModelRunnerForTesting (ModelDataCollectionInfo modelDCInfo, List<String> multipleRunCombinerReporters) throws NetLogoLinkException
	{
		return new ModelRunner(modelDCInfo,multipleRunCombinerReporters);				
	}
	
	public static class ModelRunnerException extends NetLogoLinkException
	{
		private static final long serialVersionUID = 1L;
		private final ModelRunSetupInfo runSetup;

		public ModelRunnerException(ModelRunSetupInfo runSetup, Exception ex) {
			super("", ex);
			this.runSetup = runSetup;
		}
		
		public ModelRunSetupInfo getRunSetup()
		{
			return runSetup;
		}
		
		@Override
		public String getMessage()
		{
			return getCause().toString() +  "\n\nModel run configuration was: " + runSetup;
		}
	}
	


}
