package bsearch.nlogolink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.nlogo.api.MersenneTwisterFast;

import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.ModelRunner.ModelRunnerException;
import bsearch.representations.Chromosome;

public class BatchRunner implements ModelRunningService {
	
	private ModelRunnerPool modelRunnerPool;
	private ExecutorService threadPool;
	
	public BatchRunner(int numThreads,  SearchProtocolInfo protocol) throws NetLogoLinkException {
		List<String> combineReporterSourceCodes = protocol.objectives.stream().map(obj -> obj.fitnessCombineReplications).collect(Collectors.toList());

		this.modelRunnerPool = new ModelRunnerPool(protocol.modelDCInfo, combineReporterSourceCodes, numThreads);
		this.threadPool = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
	}

	@Override
	public Map<Chromosome,List<SingleRunResult>> doBatchRun(Map<Chromosome,Integer> desiredRuns, MersenneTwisterFast rng) throws NetLogoLinkException, InterruptedException
	{
		List<Chromosome> pointsToEvaluate = new ArrayList<Chromosome>();
		for (Chromosome point : desiredRuns.keySet())
		{
			pointsToEvaluate.addAll(Collections.nCopies(desiredRuns.get(point), point ));
		}
		
		List<ModelRunSetupInfo> setups = pointsToEvaluate.stream()
				                  .map(pt -> new ModelRunSetupInfo(rng.nextInt(), pt.getParamSettings()))
				                  .collect(Collectors.toList());
		
//		List<ModelRunSetupInfo> setupList = new ArrayList<ModelRunSetupInfo>(pointsToEvaluate.size());
//		for (Chromosome point: pointsToEvaluate)
//		{
//			setupList.add(new ModelRunSetupInfo(rng.nextInt(), point.getParamSettings()));
//		}
		
		ArrayList<ModelRunnerTask> tasks = new ArrayList<ModelRunnerTask>();
		
		for (ModelRunSetupInfo setup: setups)
		{
			tasks.add(new ModelRunnerTask(modelRunnerPool, setup));
		}
		List<Future<SingleRunResult>> futures = new ArrayList<Future<SingleRunResult>>(tasks.size());
		Map<Chromosome,List<SingleRunResult>> resultsMap = new LinkedHashMap<Chromosome,List<SingleRunResult>>();
		for (Chromosome keyPoint : desiredRuns.keySet()) {
			resultsMap.put(keyPoint, new ArrayList<SingleRunResult>(desiredRuns.get(keyPoint)));
		}
		
		try {
			for (ModelRunnerTask task : tasks)
			{
				futures.add(threadPool.submit(task));
			}
			
			for (int i = 0; i < pointsToEvaluate.size(); i++)
			{
				resultsMap.get(pointsToEvaluate.get(i)).add(futures.get(i).get());
			}

			return resultsMap;
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
	public List<Object> getCombinedResultsForEachObjective(List<SingleRunResult> resultList) throws NetLogoLinkException {
		ModelRunner runner = modelRunnerPool.acquireModelRunner();
		List<Object> combinedResults;
		try {
			combinedResults = runner.evaluateCombineReplicateReporters(resultList);
		} finally {
			modelRunnerPool.releaseModelRunner(runner);
		}
		return combinedResults;
	}

	
	
	public void dispose() throws InterruptedException
	{
		threadPool.shutdownNow();
		modelRunnerPool.disposeAllRunners();		
	}
}
