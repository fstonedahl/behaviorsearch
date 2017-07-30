package bsearch.nlogolink;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

import bsearch.datamodel.ModelDataCollectionInfo;
import bsearch.util.GeneralUtils;

public class ModelRunnerPool {
	private List<ModelRunner> allModelRunners = Collections.synchronizedList(new java.util.LinkedList<ModelRunner>());
	private Queue<ModelRunner> unusedModelRunners = new java.util.concurrent.ConcurrentLinkedQueue<ModelRunner>();
	private ModelDataCollectionInfo modelDCInfo;
	List<String> combineReporterSourceCodes;
	
	public ModelRunnerPool(ModelDataCollectionInfo modelDCInfo, List<String> combineReporterSourceCodes) {
		this.modelDCInfo = modelDCInfo;		
		this.combineReporterSourceCodes = combineReporterSourceCodes;
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
		ModelRunner runner = new ModelRunner(modelDCInfo, combineReporterSourceCodes);
		
		allModelRunners.add(runner);
		GeneralUtils.debug("allModelRunners.size()== " + allModelRunners.size());
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
