package bsearch.nlogolink;



public class ModelRunnerTask implements java.util.concurrent.Callable<SingleRunResult> {
	ModelRunSetupInfo runSetup;
	ModelRunnerPool pool;

	public ModelRunnerTask(ModelRunnerPool pool, ModelRunSetupInfo runSetup) {
		super();
		this.pool = pool;
		this.runSetup = runSetup;
	}

	public SingleRunResult call() throws NetLogoLinkException, ModelRunner.ModelRunnerException 
	{
		ModelRunner runner = pool.acquireModelRunner();
		SingleRunResult result = runner.doFullRun(runSetup);
		pool.releaseModelRunner(runner);
		return result;
	}
}
