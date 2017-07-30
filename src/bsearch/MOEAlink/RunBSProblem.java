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

import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.evaluation.StandardFitnessFunction;
import bsearch.nlogolink.BatchRunner;
import bsearch.nlogolink.NLogoUtils;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.util.GeneralUtils;

public class RunBSProblem {

	public static void main(String[] args) throws IOException, SAXException, NetLogoLinkException {
//		Instrumenter instrumenter = new Instrumenter()
//				.withProblemClass(SchafferProblem.class)
//				.withFrequency(1000)
//				.attachElapsedTimeCollector();
		
		String FILENAME = GeneralUtils.attemptResolvePathFromBSearchRoot("test/TesterMOEA.bsearch") ;

		GeneralUtils.updateProtocolFolder(FILENAME);

		SearchProtocolInfo protocol = SearchProtocolInfo.loadOldXMLBasedFile( FILENAME ) ;
		//Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(protocol);
		System.out.println(json);
		json = "{\"bsearchVersionNumber\":0.72,\"modelFile\":\"TesterMOEA.nlogo\",\"modelStepCommands\":\"go\",\"modelSetupCommands\":\"setup\",\"modelStopCondition\":\"count turtles \\u003e 1000\",\"modelStepLimit\":0,\"modelMetricReporter\":\"ellipsoid-slow\",\"modelMeasureIf\":\"true\",\"paramSpecStrings\":[\"[\\\"a\\\" [-32 1 32]]\",\"[\\\"b\\\" [-32 1 32]]\",\"[\\\"c\\\" [-32 1 32]]\",\"[\\\"d\\\" [-32 1 32]]\"],\"fitnessMinimized\":true,\"fitnessSamplingReplications\":8,\"fitnessCollecting\":\"last @{MEASURE1}\",\"fitnessCombineReplications\":\"MEAN\",\"fitnessDerivativeParameter\":\"\",\"fitnessDerivativeDelta\":0.0,\"fitnessDerivativeUseAbs\":false,\"searchMethodType\":\"MutationHillClimber\",\"searchMethodParams\":{\"mutation-rate\":\"0.05\",\"restart-after-stall-count\":\"0\"},\"chromosomeType\":\"GrayBinaryChromosome\",\"caching\":true,\"evaluationLimit\":60,\"bestCheckingNumReplications\":5}";
		SearchProtocolInfo protocol2 = gson.fromJson(json,SearchProtocolInfo.class);
		System.out.println(gson.toJson(protocol2));
		

		//protocol2.save(new FileWriter(GeneralUtils.attemptResolvePathFromBSearchRoot("test/TesterMOEA.bsearch2")));
		
		System.exit(0);
		
		
    	int numEvaluationThreads = 1;
		BatchRunner batchRunner = new BatchRunner(numEvaluationThreads,protocol);

		SearchManager manager = new SearchManager(0, batchRunner, protocol, new StandardFitnessFunction(protocol.objectives.get(0)));

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
				.withMaxEvaluations(protocol.searchAlgorithmInfo.evaluationLimit)
				.distributeOn(1)
				.withProgressListener(progListener)
				.run();
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
