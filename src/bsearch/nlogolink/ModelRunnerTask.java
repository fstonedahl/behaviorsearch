package bsearch.nlogolink;



public class ModelRunnerTask implements java.util.concurrent.Callable<ModelRunResult> {
	ModelRunSetupInfo runSetup;
	ModelRunnerPool factory;

	public ModelRunnerTask(ModelRunnerPool factory, ModelRunSetupInfo runSetup) {
		super();
		this.factory = factory;
		this.runSetup = runSetup;
	}

	public ModelRunResult call() throws NetLogoLinkException, ModelRunner.ModelRunnerException 
	{
		ModelRunner runner = factory.acquireModelRunner();
		ModelRunResult result = runner.doFullRun(runSetup);
		factory.releaseModelRunner(runner);
		return result;
	}
}
