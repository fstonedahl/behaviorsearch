package bsearch.nlogolink;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;

import org.nlogo.api.AgentException;
import org.nlogo.api.CompilerException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.Procedure;
import org.nlogo.util.MersenneTwisterFast;
import org.nlogo.headless.HeadlessWorkspace;

public strictfp class ModelRunner {

	private HeadlessWorkspace workspace;

	private Procedure setupCommands;
	private Procedure stepCommands;
	private Procedure stopConditionReporter;
	private Procedure measureIfReporter = null;
	private final boolean recordEveryTick;
	private final int maxModelSteps;
	 
	// Note: ModelRunner can collect multiple measures, for eventual support for extending to multi-objective optimization.
	private LinkedHashMap<String,Procedure> resultReporters = new LinkedHashMap<String,Procedure>();
		
	private boolean runIsDone; 
	
	//private MersenneTwisterFast myRNG; 
	
	private ModelRunner(String modelFileName, boolean recordEveryTick, int maxModelSteps) 
		throws LogoException, IOException, CompilerException 
	{
		workspace = Utils.createWorkspace();
		workspace.open(modelFileName);
		 
		//myRNG = workspace.world.mainRNG.clone();
		
		this.recordEveryTick = recordEveryTick;
		this.maxModelSteps = maxModelSteps;
	}

	public void setSetupCommands(String commands) throws CompilerException
	{
		setupCommands = workspace.compileCommands(commands);
	}
	public void setStepCommands(String commands) throws CompilerException
	{
		stepCommands = workspace.compileCommands(commands);
	}
	public void setStopConditionReporter(String reporter) throws CompilerException
	{
		if (reporter.trim().length() > 0)
		{
			stopConditionReporter = workspace.compileReporter(reporter);
		}
	}	
	public void addResultReporter(String reporter) throws CompilerException
	{
		resultReporters.put(reporter, workspace.compileReporter(reporter));
	}
	public void setMeasureIfReporter(String reporter) throws CompilerException
	{
		if (reporter.trim().length() > 0)
		{
			measureIfReporter = workspace.compileReporter(reporter);
		}
	}
	public boolean checkStopCondition()
	{
		return (stopConditionReporter != null) &&
			(Boolean) workspace.runCompiledReporter( stopConditionReporter , workspace.world.mainRNG.clone());
	}
	
	public void setup(long seed, LinkedHashMap<String,Object> parameterSettings ) throws LogoException, AgentException
	{
		workspace.clearAll();
		for (String s: parameterSettings.keySet())
		{
			workspace.world.setObserverVariableByName(s, parameterSettings.get(s));		
		}
		try {
			workspace.world.mainRNG.setSeed( seed );
		} catch (Exception ex) {ex.printStackTrace(); }

		workspace.runCompiledCommands(setupCommands);
		
		runIsDone = checkStopCondition();		
	}
	
	/** returns true if the run is finished **/
	public boolean go()
	{
		workspace.runCompiledCommands(stepCommands );		

		runIsDone = checkStopCondition();
		return runIsDone;
	}
	
	public LinkedHashMap<String,Object> measureResults() throws NetLogoLinkException
	{
		LinkedHashMap<String,Object> results = new LinkedHashMap<String,Object>();
		for (String key: resultReporters.keySet())
		{
			results.put(key, measureResultReporter(resultReporters.get(key)));
		}		
		return results;
	}
	
	private Double measureResultReporter(Procedure reporter) throws NetLogoLinkException
	{
		Object obj = null;
		try {
			obj = workspace.runCompiledReporter( reporter , workspace.world.mainRNG.clone());
			return (Double) obj;
		} catch (ClassCastException ex)
		{
			ex.printStackTrace();
			throw new NetLogoLinkException("Result reporters must return numeric results!  Invalid reported value was: " + obj );
		}
	}

	private void conditionallyRecordResults(ModelRunResult results) throws NetLogoLinkException
	{
		if ((measureIfReporter == null) || (workspace.runCompiledReporter( measureIfReporter , workspace.world.mainRNG.clone()).equals(Boolean.TRUE)))
		{
			for (String key: resultReporters.keySet())
			{
				results.addResult(key, measureResultReporter(resultReporters.get(key)));
			}
		}		
	}
	public ModelRunResult doFullRun(RunSetup runSetup) throws ModelRunnerException
	{
		try {
			ModelRunResult results = new ModelRunResult(runSetup.seed);
			setup(runSetup.seed, runSetup.parameterSettings);
			
			int steps;
			for (steps = 0; steps < maxModelSteps && !runIsDone; steps++)
			{
				if (recordEveryTick )
				{
					conditionallyRecordResults(results);
				}
				go();
			}
			conditionallyRecordResults(results);
			if (results.isEmpty())
			{
				throw new NetLogoLinkException("No metric values were collected during this model run! (Model was run for " + steps + " steps.)");
			}
			return results;
		}
		catch (Exception ex)  {		
			throw new ModelRunnerException(runSetup, ex);
		}
	}
	
	public boolean isRunDone()
	{
		return runIsDone;
	}

	public void dispose() throws InterruptedException {
		workspace.dispose();		
	}

	/** Just used for testing - preferable to use the Factory paradigm  */
	public static ModelRunner createModelRunnerForTesting (String s, boolean recordEveryTick, int maxModelSteps) throws LogoException, IOException, CompilerException
	{
		return new ModelRunner(s, recordEveryTick, maxModelSteps);				
	}
	
	
	/** Note: This uses a cloned random number generator, so it 
	 * won't affect the state of the main RNG for the run.  
	 * Still, be careful because some reporters could have side-effects in the model
	 * (for instance, if the reporter creates some agents 
	 * (even if it kills them before its done, the "who" numbers of agents 
	 *  created after that will be different...) 
	 * @param reporter	the NetLogo reporter expression to run.
	 * @return 			the result of running the reporter
	 * @throws CompilerException
	 */
	public Object report(String reporter) throws CompilerException
	{
		Procedure procReporter = workspace.compileReporter( reporter );
		return workspace.runCompiledReporter( procReporter, workspace.world.mainRNG.clone() );
	}
	/**
	 * Note: this method uses the main random-number generator, and thus 
	 * will affect the state of the RNG for future calls.
	 * @param cmd 
	 * @throws CompilerException
	 * @throws LogoException
	 */
	public void command(String cmd) throws CompilerException, LogoException
	{
		workspace.command( cmd );
	}

	public static class ModelRunnerException extends NetLogoLinkException
	{
		private static final long serialVersionUID = 1L;
		private final RunSetup runSetup;

		public ModelRunnerException(RunSetup runSetup, Exception ex) {
			super("", ex);
			this.runSetup = runSetup;
		}
		
		public RunSetup getRunSetup()
		{
			return runSetup;
		}
		
		@Override
		public String getMessage()
		{
			return getCause().toString() +  "\n\nModel run configuration was: " + runSetup;
		}
	}
	
	public static class RunSetup {
		final long seed;
		final private LinkedHashMap<String,Object> parameterSettings;
		
		public RunSetup(long seed,
				LinkedHashMap<String, Object> parameterSettings) {
			super();
			this.seed = seed;
			this.parameterSettings = parameterSettings;
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("{RANDOM-SEED: " + seed + ",\n  SETTINGS: {");
			for (String key : parameterSettings.keySet())
			{
				sb.append(key + "=" + org.nlogo.api.Dump.logoObject(parameterSettings.get(key), true, false) + ", ");
			}
			sb.append("}}"); 
			return sb.toString();
		}
	}

	public static class Factory {
		private List<ModelRunner> allModelRunners = Collections.synchronizedList(new java.util.LinkedList<ModelRunner>());
		private Queue<ModelRunner> unusedModelRunners = new java.util.concurrent.ConcurrentLinkedQueue<ModelRunner>();
		
		private final String modelFileName, setupCommands, stepCommands, stopCondition, metricReporter, measureIfReporter;
		private final boolean recordEveryTick;
		private final int maxModelSteps;		
		
		public Factory(String modelFileName, boolean recordEveryTick, int maxModelSteps, 
				String setupCommands, String stepCommands, String stopCondition, String metricReporter, 
				String measureIfReporter)
		{
			this.modelFileName = modelFileName;
			this.setupCommands = setupCommands;
			this.stepCommands = stepCommands;
			this.stopCondition = stopCondition;
			this.metricReporter = metricReporter;
			this.measureIfReporter = measureIfReporter;
			this.recordEveryTick = recordEveryTick;
			this.maxModelSteps = maxModelSteps;
			
		}
		/** Either creates a new one, or recycles a ModelRunner that has been released again into the pool */
		public ModelRunner acquireModelRunner() throws NetLogoLinkException 
		{
			ModelRunner runner = unusedModelRunners.poll();
			if (runner != null)
			{
				return runner;
			}
			else
			{
				return newModelRunner();
			}
		}
		public void releaseModelRunner(ModelRunner runner)
		{
			unusedModelRunners.add(runner);
		}
		
		private ModelRunner newModelRunner() throws NetLogoLinkException
		{
			ModelRunner runner;
	    	try {
				runner = new ModelRunner(modelFileName, recordEveryTick, maxModelSteps);
	    	} catch (LogoException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error opening NetLogo model.  NetLogo sent back this error message: \"" + e.getMessage() + "\"");
			} catch (IOException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("I/O error when loading NetLogo model ( " + modelFileName + " ).  Error message: \"" + e.getMessage() + "\"");
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling NetLogo model ( " + modelFileName + " ).  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
	
	        try {
				runner.setSetupCommands(setupCommands);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's setup commands : " +setupCommands.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
			
	        try {
				runner.setStepCommands(stepCommands);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's step commands : " +stepCommands.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
	        try {
				runner.setStopConditionReporter(stopCondition );
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's stop condition: " +stopCondition.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
			try {
				runner.setMeasureIfReporter(measureIfReporter);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's measure-if condition: " +measureIfReporter.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
	        try {
				runner.addResultReporter(metricReporter);
			} catch (CompilerException e) {
				e.printStackTrace();
				throw new NetLogoLinkException("Error compiling the model's fitness metric: " +metricReporter.toUpperCase() + " \n  NetLogo's error message: \"" + e.getMessage() + "\"");
			}
			
			allModelRunners.add(runner);
			
			return runner;			
		}

		/** This method should only be called after you are done using this Factory, and all the ModelRunners created by it.
		 * It disposes the ModelRunners (and their corresponding NetLogo workspaces), and attempting to use them after this
		 * will result in errors.
		 * */
		public void disposeAllRunners() throws InterruptedException {
			synchronized(allModelRunners)
			{
				for (ModelRunner runner : allModelRunners) {
					runner.dispose();
				}
				allModelRunners.clear();
			}
		}

	}

}
