package bsearch.nlogolink;

import java.util.ArrayList;
import java.util.Collections;
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

	public BatchRunner(int numThreads, SearchProtocolInfo protocol, boolean storeRawResults) throws NetLogoLinkException {
		List<String> combineReporterSourceCodes = protocol.objectives.stream()
				.map(obj -> obj.fitnessCombineReplications).collect(Collectors.toList());

		this.modelRunnerPool = new ModelRunnerPool(protocol.modelDCInfo, combineReporterSourceCodes, numThreads, storeRawResults);
		this.threadPool = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
	}

	@Override
	public LinkedHashMap<Chromosome, MultipleRunResult> doBatchRun(Map<Chromosome, Integer> desiredRuns,
			MersenneTwisterFast rng) throws NetLogoLinkException, InterruptedException {
		List<Chromosome> pointsToEvaluate = new ArrayList<Chromosome>();
		for (Chromosome point : desiredRuns.keySet()) {
			pointsToEvaluate.addAll(Collections.nCopies(desiredRuns.get(point), point));
		}

		List<ModelRunSetupInfo> setups = pointsToEvaluate.stream()
				.map(pt -> new ModelRunSetupInfo(rng.nextInt(), pt.getParamSettings())).collect(Collectors.toList());

		ArrayList<ModelRunnerTask> tasks = new ArrayList<ModelRunnerTask>();

		for (ModelRunSetupInfo setup : setups) {
			tasks.add(new ModelRunnerTask(modelRunnerPool, setup));
		}
		List<Future<SingleRunResult>> futures = new ArrayList<Future<SingleRunResult>>(tasks.size());
		LinkedHashMap<Chromosome, MultipleRunResult> resultsMap = new LinkedHashMap<>();
		for (Chromosome keyPoint : desiredRuns.keySet()) {
			resultsMap.put(keyPoint, new MultipleRunResult(desiredRuns.get(keyPoint)));
		}

		try {
			// perform the NetLogo model runs in parallel
			for (ModelRunnerTask task : tasks) {
				futures.add(threadPool.submit(task));
			}

			for (int i = 0; i < pointsToEvaluate.size(); i++) {
				resultsMap.get(pointsToEvaluate.get(i)).addSingleRun(futures.get(i).get());
			}

			// evaluate all of the "combined" measures for each set of replicate runs
			ModelRunner runner = modelRunnerPool.acquireModelRunner();
			for (Chromosome point : resultsMap.keySet()) {
				MultipleRunResult multiResults = resultsMap.get(point);
				List<SingleRunResult> resultList = multiResults.getSingleRuns();
				multiResults.setCombinedMeasures(runner.evaluateCombineReplicateReporters(resultList));
			}
			modelRunnerPool.releaseModelRunner(runner);

			return resultsMap;
		} catch (ExecutionException ex) {
			if (ex.getCause() instanceof NetLogoLinkException) {
				throw (NetLogoLinkException) ex.getCause();
			} else if (ex.getCause() instanceof ModelRunnerException) {
				throw (ModelRunnerException) ex.getCause();
			} else {
				ex.printStackTrace();
				throw new NetLogoLinkException("Unexpected exception while attempting to run NetLogo", ex);
			}
		}
	}

	public void dispose() throws InterruptedException {
		threadPool.shutdownNow();
		modelRunnerPool.disposeAllRunners();
	}
 
	private class ModelRunnerTask implements java.util.concurrent.Callable<SingleRunResult> {
		ModelRunSetupInfo runSetup;

		public ModelRunnerTask(ModelRunnerPool pool, ModelRunSetupInfo runSetup) {
			super();
			this.runSetup = runSetup;
		}

		public SingleRunResult call() throws NetLogoLinkException, ModelRunner.ModelRunnerException {
			ModelRunner runner = modelRunnerPool.acquireModelRunner();
			SingleRunResult result = runner.doFullRun(runSetup);
			modelRunnerPool.releaseModelRunner(runner);
			return result;
		}
	}

}
