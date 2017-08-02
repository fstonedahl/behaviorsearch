package bsearch.MOEAlink;

import java.io.IOException;
import org.moeaframework.Executor;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import bsearch.app.BehaviorSearchException;
import bsearch.app.CSVLoggerListener;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ObjectiveEvaluator;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.BatchRunner;
import bsearch.nlogolink.NLogoUtils;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;

public class RunBSProblem {

	public static void main(String[] args) throws IOException, SAXException, BehaviorSearchException {
//		Instrumenter instrumenter = new Instrumenter()
//				.withProblemClass(SchafferProblem.class)
//				.withFrequency(1000)
//				.attachElapsedTimeCollector();
		
		String FILENAME = GeneralUtils.attemptResolvePathFromBSearchRoot("test/TesterMOEA.bsearch") ;

		GeneralUtils.updateProtocolFolder(FILENAME);

		SearchProtocolInfo protocol = SearchProtocolInfo.loadOldXMLBasedFile( FILENAME ) ;
				
		int numEvaluationThreads = 4;
		BatchRunner batchRunner = new BatchRunner(numEvaluationThreads,protocol);

		SearchManager manager = new SearchManager(0, batchRunner, protocol, new ObjectiveEvaluator(protocol.objectives));
		ResultListener csvListener = new CSVLoggerListener(protocol, "test/tmptmp/TesterMOEA", true, true, true, true);
		manager.addResultsListener(csvListener);
		csvListener.initListener(new SearchSpace(protocol.paramSpecStrings), protocol);
		csvListener.searchStarting(manager.getStatsKeeper());
		
		Plot plot = new Plot();

		ProgressListener progListener = new ProgressListener() {			
			@Override
			public void progressUpdate(ProgressEvent event) {
//				if (event.getCurrentNFE() % 10 != 0) {
//					return;
//				}
				System.out.println("nfe: " + event.getCurrentNFE());
				NondominatedPopulation tempResult = event.getCurrentAlgorithm().getResult();
				System.out.println("            size: " + tempResult.size());
				Solution oneOfBest = tempResult.get(0); 
				System.out.println("            obj: " + oneOfBest.getObjective(0));
				
				plot.add("NFE"+event.getCurrentNFE(), tempResult);
				//						Solution solution = event.getCurrentAlgorithm().getResult().get(0);
//				System.out.printf("%.5f => %.5f, %.5f\n", 
//						EncodingUtils.getReal(solution.getVariable(0)),
//						solution.getObjective(0), solution.getObjective(1));						
//				System.out.println("result= " + sol.toString());
				
//				Accumulator acc = instrumenter.getLastAccumulator();
//				for (String key : acc.keySet()) {
//					System.out.println("   " + key + ": " + acc.get(key, acc.size(key) - 1));
//				}
				
			}
		};
		long startTime = System.currentTimeMillis();
		NondominatedPopulation result = new Executor().withAlgorithm("GA")
				.withProblem(new BSProblem(protocol,manager))
				.withProperty("populationSize", 10)
//				.withInstrumenter(instrumenter)
				.withMaxEvaluations(protocol.searchAlgorithmInfo.evaluationLimit)
				.distributeOn(6)
				.withProgressListener(progListener)
				.run();
		csvListener.searchFinished(manager.getStatsKeeper());
		csvListener.allSearchesFinished();
		long endTime = System.currentTimeMillis();
		//System.out.println(endTime-startTime);
		plot.show();
		
		
		try {
			batchRunner.dispose();
			NLogoUtils.fullyShutDownNetLogoLink();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		for (Solution solution : result) {
//			System.out.printf("%.5f => %.5f, %.5f\n", 
//					EncodingUtils.getReal(solution.getVariable(0)),
//					solution.getObjective(0), solution.getObjective(1));
//		}

//		new Plot()
//			.add("NSGAII", result)
//			.show();
//		
//		new Plot()
//			.add(instrumenter.getLastAccumulator())
//			.show();
	}

}
