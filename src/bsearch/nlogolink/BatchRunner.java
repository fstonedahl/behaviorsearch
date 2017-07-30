package bsearch.nlogolink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;

public class BatchRunner implements ModelRunningService {
	
	private ModelRunnerPool modelRunnerPool;
	private ExecutorService threadPool;
	
	public BatchRunner(int numThreads,  SearchProtocolInfo protocol) throws NetLogoLinkException {
		List<String> combineReporterSourceCodes = protocol.objectives.stream().map(obj -> obj.fitnessCombineReplications).collect(Collectors.toList());

		this.modelRunnerPool = new ModelRunnerPool(protocol.modelDCInfo, combineReporterSourceCodes);
		this.threadPool = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
	}

	@Override
	public List<ModelRunResult> doBatchRun(List<ModelRunSetupInfo> setups) throws NetLogoLinkException, InterruptedException
	{
		ArrayList<ModelRunnerTask> tasks = new ArrayList<ModelRunnerTask>();
		
		for (ModelRunSetupInfo setup: setups)
		{
			tasks.add(new ModelRunnerTask(modelRunnerPool, setup));
		}
		List<Future<ModelRunResult>> futures = new ArrayList<Future<ModelRunResult>>(tasks.size());
		List<ModelRunResult> results = new ArrayList<ModelRunResult>(tasks.size());
		
		try {
			for (ModelRunnerTask task : tasks)
			{
				futures.add(threadPool.submit(task));
			}
			
			for (Future<ModelRunResult> future : futures)
			{
				results.add(future.get());
			}

			return results;		
		}  
		catch (ExecutionException ex) {
			if (ex.getCause() instanceof NetLogoLinkException)
			{
				throw (NetLogoLinkException) ex.getCause();
			}
			else if (ex.getCause() instanceof ModelRunnerException)
			{
				throw (ModelRunnerException) ex.getCause();
			}
			else
			{
				ex.printStackTrace();
				throw new NetLogoLinkException("Unexpected exception while attempting to run NetLogo", ex);
			}
		}
	}
		
	/**
	 * 
	 * @param resultList - list of result data for each model run
	 * @return a List containing the result of *combining* across replicate model runs. 
	 *         The resulting list contains one item for each objective. 
	 * @throws NetLogoLinkException
	 */
	@Override
	public List<Object> getCombinedResultsForEachObjective(List<ModelRunResult> resultList) throws NetLogoLinkException {
		ModelRunner runner = modelRunnerPool.acquireModelRunner();
//		ModelRunner runner = modelRunnerPool.getExtraRunner();
		List<Object> combinedResults = runner.evaluateCombineReplicateReporters(resultList);
		modelRunnerPool.releaseModelRunner(runner);
		return combinedResults;
	}

	
	
	public void dispose() throws InterruptedException
	{
		threadPool.shutdownNow();
		modelRunnerPool.disposeAllRunners();		
	}
}
