package bsearch.nlogolink;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;

import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.ModelDataCollectionInfo;
import bsearch.util.GeneralUtils;

public class ModelRunnerPool {
	private List<ModelRunner> allModelRunners = Collections.synchronizedList(new java.util.LinkedList<ModelRunner>());
	private BlockingDeque<ModelRunner> availableModelRunners;
	
	public ModelRunnerPool(ModelDataCollectionInfo modelDCInfo, List<String> combineReporterSourceCodes, int fixedCapacity) throws NetLogoLinkException {
		this.availableModelRunners = new java.util.concurrent.LinkedBlockingDeque<ModelRunner>(fixedCapacity);

		for (int i = 0; i < fixedCapacity; i++) {
			allModelRunners.add(new ModelRunner(modelDCInfo, combineReporterSourceCodes));
		}
		availableModelRunners.addAll(allModelRunners);
		
	}
	/** Either returns an available ModelRunner, or waits (blocks) until it can do so 
	 * @throws NetLogoLinkException */
	public ModelRunner acquireModelRunner() throws NetLogoLinkException 
	{
		try {
			return availableModelRunners.take();
		} catch (InterruptedException ex) {
			throw new NetLogoLinkException("Thread was interrupted while trying to acquire NetLogo ModelRunner from Pool.");
		}
	}
	public void releaseModelRunner(ModelRunner runner)
	{
		availableModelRunners.add(runner);
	}
	
		
	/** This method should only be called after you are done using this Pool, and all the ModelRunners created by it.
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
