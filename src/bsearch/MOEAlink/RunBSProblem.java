package bsearch.MOEAlink;

import java.io.IOException;
import java.util.Arrays;

import org.moeaframework.Executor;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;
import org.xml.sax.SAXException;

import bsearch.app.BehaviorSearch;
import bsearch.app.BehaviorSearchException;
import bsearch.app.CSVLoggerListener;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ObjectiveEvaluator;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.evaluation.SearchProgressStatsKeeper;
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
		
		String FILENAME = GeneralUtils.attemptResolvePathFromBSearchRoot("test/TesterMOEA2.bsearch2") ;

		GeneralUtils.updateProtocolFolder(FILENAME);

		//SearchProtocolInfo protocol = SearchProtocolInfo.loadOldXMLBasedFile( FILENAME ) ;
		SearchProtocolInfo protocol = SearchProtocolInfo.loadFromFile( FILENAME ) ;
				
		int numEvaluationThreads = 3;
		BatchRunner batchRunner = new BatchRunner(numEvaluationThreads,protocol,false);
		BehaviorSearch.RunOptions runOptions = new BehaviorSearch.RunOptions();
		runOptions.outputStem = "test/tmptmp/TesterMOEA2";
		ResultListener csvListener = new CSVLoggerListener(protocol, runOptions);
		csvListener.initListener(new SearchSpace(protocol.paramSpecStrings), protocol);
		
		SearchProgressStatsKeeper statsKeeper = new SearchProgressStatsKeeper(1,0,0,protocol.objectives,Arrays.asList(csvListener));

		SearchManager manager = new SearchManager(0, batchRunner, protocol, new ObjectiveEvaluator(protocol.objectives),statsKeeper);
		
		Plot plot = new Plot();

		ProgressListener progListener = new ProgressListener() {			
			@Override
			public void progressUpdate(ProgressEvent event) {
				if (event.getCurrentNFE() % 100 != 0) {
					return;
				}
				System.out.println("nfe: " + event.getCurrentNFE());
				NondominatedPopulation tempResult = event.getCurrentAlgorithm().getResult();
				System.out.println("            size: " + tempResult.size());
				Solution oneOfBest = tempResult.get(0); 
				System.out.println("            obj 0: " + oneOfBest.getObjective(0) + " obj 1: " + oneOfBest.getObjective(1));
				
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
		for (int searchNum = 1; searchNum <= 2; searchNum++) {
			statsKeeper.searchStartingEvent(searchNum);
			PRNG.setSeed(searchNum);
			NondominatedPopulation result = new Executor().withAlgorithm("NSGA2")
					.withProblem(new BSProblem(protocol,manager))
					.withProperty("populationSize", 20)
	//				.withInstrumenter(instrumenter)
					.withMaxEvaluations(protocol.searchAlgorithmInfo.evaluationLimit / protocol.modelDCInfo.fitnessSamplingReplications)
					.distributeOn(2)
					.withProgressListener(progListener)
					.run();
			System.out.println("final pop size= " + result.size());
			statsKeeper.searchFinishedEvent();
		}
		statsKeeper.allSearchesFinishedEvent();
		plot.show();
		
		long endTime = System.currentTimeMillis();
		//System.out.println(endTime-startTime);
		
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
