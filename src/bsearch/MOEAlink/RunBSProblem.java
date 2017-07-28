package bsearch.MOEAlink;

import java.io.IOException;

import org.moeaframework.Executor;
import org.moeaframework.Instrumenter;
import org.moeaframework.analysis.collector.Accumulator;
import org.moeaframework.analysis.plot.Plot;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.util.progress.ProgressEvent;
import org.moeaframework.util.progress.ProgressListener;
import org.xml.sax.SAXException;

import bsearch.app.SearchProtocol;
import bsearch.evaluation.SearchManager;
import bsearch.evaluation.StandardFitnessFunction;
import bsearch.nlogolink.BatchRunner;
import bsearch.nlogolink.ModelRunner;
import bsearch.nlogolink.Utils;
import bsearch.util.GeneralUtils;

public class RunBSProblem {

	public static void main(String[] args) throws IOException, SAXException {
//		Instrumenter instrumenter = new Instrumenter()
//				.withProblemClass(SchafferProblem.class)
//				.withFrequency(1000)
//				.attachElapsedTimeCollector();
		
		String FILENAME = GeneralUtils.attemptResolvePathFromBSearchRoot("test/TesterMOEA.bsearch") ;

		GeneralUtils.updateProtocolFolder(FILENAME);

		SearchProtocol protocol = SearchProtocol.loadFile( FILENAME ) ;

		boolean measureEveryTick = !protocol.fitnessCollecting.equals(SearchProtocol.FITNESS_COLLECTING.AT_FINAL_STEP);
		ModelRunner.Factory mrunnerFactory = new ModelRunner.Factory(
				GeneralUtils.attemptResolvePathFromProtocolFolder(protocol.modelFile), measureEveryTick, 
				protocol.modelStepLimit, protocol.modelSetupCommands, protocol.modelStepCommands, 
				protocol.modelStopCondition, protocol.modelMetricReporter, protocol.modelMeasureIf);
    	int numEvaluationThreads = 1;
		BatchRunner batchRunner = new BatchRunner(numEvaluationThreads, mrunnerFactory);

		SearchManager manager = new SearchManager(0, batchRunner, protocol, new StandardFitnessFunction(protocol),
				false, 0);

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
				.withProperty("populationSize", 20)
//				.withInstrumenter(instrumenter)
				.withMaxEvaluations(protocol.evaluationLimit)
				.distributeOn(1)
				//.withProgressListener(progListener)
				.run();
		long endTime = System.currentTimeMillis();
		System.out.println(endTime-startTime);
		//plot.show();
		
		
		try {
			batchRunner.dispose();
			Utils.fullyShutDownNetLogoLink();
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
