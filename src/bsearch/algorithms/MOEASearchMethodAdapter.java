package bsearch.algorithms;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.moeaframework.Executor;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.TerminationCondition;
import bsearch.MOEAlink.BSProblem;
import bsearch.app.BehaviorSearchException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.SearchManager;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.ChromosomeFactory;
import bsearch.space.SearchSpace;
import bsearch.util.OrderedTypedProperties;

public class MOEASearchMethodAdapter implements SearchMethod {
	
	private String algorithmName;
	private OrderedTypedProperties props = null;
	
	public MOEASearchMethodAdapter(String algorithmName) {
		this.algorithmName = algorithmName;		
	}
	@Override
	public void updateSearchParams(Map<String, String> searchMethodParams) throws SearchParameterException {
		if (props == null) {
			props = new OrderedTypedProperties();
		} 
		props.putAll(searchMethodParams);
	}

	@Override
	public Map<String, String> getSearchParams() {
		if (props == null) {
			props = new OrderedTypedProperties();
			if (algorithmName.equals("NSGA-II")) {
				props.setInt("populationSize", 100);
				props.setBoolean("withReplacement", true);
			} else if (algorithmName.equals("Random")) {
				props.setInt("populationSize", 100);
			}
		}
		
		return props.asOrderedMap();			
	}

	@Override
	public HashMap<String, String> getSearchParamsHelp() {
		LinkedHashMap<String,String> helpMap = new LinkedHashMap<>();
		if (algorithmName.equals("NSGA-II")) {
			helpMap.put("populationSize", "The maximum populuation size");
			helpMap.put("withReplacement", "Should tournament selection be with replacement, or without?");
		} else if (algorithmName.equals("Random")) {
			helpMap.put("populationSize", "Using a 'population' allows multiple random points to be evaluated in parallel (if using multi-threaded evaluation.");
		} else {
			helpMap.put("-", "Sorry, no parameter help is available for this algorithm.  Refer to MOEA library reference?");
		} 

		return helpMap;		
	}

	@Override
	public String getName() {
		return algorithmName;
	}

	@Override
	public String getDescription() {
		return null; //TODO: Fix descriptions of search algorithms!
	}

	@Override
	public String getHTMLHelpText() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("<HTML><BODY><B>%s:</B> %s", this.getName(), this.getDescription()));
		if (this.getSearchParams().size() > 0)
		{
			sb.append("<UL>");
			
			for (String pname: this.getSearchParamsHelp().keySet())
			{
				sb.append(String.format("<LI><I>%s</I> - %s", pname, this.getSearchParamsHelp().get(pname)));
			}
			sb.append("</UL></BODY></HTML>");
		}		
		sb.append("<P>(This algorithm comes from the <A HREF='http://moeaframework.org/'>MOEA framework</A>.  "
				+ "The list of properties above is not comprehenesive.  "
				+ "Refer to Appendices A-C of the MOEA Beginner's Guide for more details about the properties "
				+ "for this algorithm, and which mutation/crossover operators each algorithm supports.)</P>");
		return sb.toString();
	}

	@Override
	public void search(SearchSpace space, ChromosomeFactory cFactory, SearchProtocolInfo protocol, SearchManager manager,
			int randomSeed, int numEvaluationThreads) throws BehaviorSearchException, NetLogoLinkException{
 
		PRNG.setSeed(randomSeed);
		
		TerminationCondition terminationCondition = new TerminationCondition() {			
			@Override
			public void initialize(Algorithm algorithm) {}
			
			@Override
			public boolean shouldTerminate(Algorithm algorithm) {
				return manager.searchFinished();
			}
		};
		
		NondominatedPopulation result = new Executor().withAlgorithm(this.algorithmName)
				.withProblem(new BSProblem(protocol,manager,randomSeed))
				.withProperties(props.asProperties())
				.distributeOn(numEvaluationThreads)
				//.withMaxEvaluations(protocol.searchAlgorithmInfo.evaluationLimit / protocol.modelDCInfo.fitnessSamplingReplications)
				.withTerminationCondition(terminationCondition)
				.run();
		

	}

}
