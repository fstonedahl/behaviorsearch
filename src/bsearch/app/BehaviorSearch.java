package bsearch.app;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.nlogo.api.MersenneTwisterFast;
import org.xml.sax.SAXException;

import bsearch.algorithms.SearchMethod;
import bsearch.algorithms.SearchMethodLoader;
import bsearch.algorithms.SearchParameterException;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.evaluation.ObjectiveEvaluator;
import bsearch.evaluation.ResultListener;
import bsearch.evaluation.SearchManager;
import bsearch.evaluation.SearchProgressStatsKeeper;
import bsearch.nlogolink.BatchRunner;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.nlogolink.NLogoUtils;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.space.ParameterSpec;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;

/**
 * NOTES:
 *  TODO: Eventually handle setting world-dimensions? and random-seed? (prob. not needed)
 *
 */
public strictfp class BehaviorSearch {

	public static void runMultipleSearches(SearchProtocolInfo protocol, List<ResultListener> listeners, RunOptions runOptions)
			throws BehaviorSearchException, InterruptedException, SearchParameterException {

    	BatchRunner batchRunner = new BatchRunner(runOptions.numThreads, protocol, runOptions.verboseModelOutput);

		
		SearchSpace space = new SearchSpace(protocol.paramSpecStrings);
		for (ResultListener listener : listeners) {
			listener.initListener(space, protocol);
		} 

		SearchProgressStatsKeeper statsKeeper = new SearchProgressStatsKeeper(runOptions.firstSearchNumber,0,0,protocol.objectives,listeners);
		
    	int endSearchNumber = runOptions.firstSearchNumber + runOptions.numSearches;
    	for (int searchNumber = runOptions.firstSearchNumber; searchNumber < endSearchNumber; searchNumber++)
    	{
    		int randomSeed = runOptions.randomSeed + (searchNumber - runOptions.firstSearchNumber);
    		MersenneTwisterFast rng = new MersenneTwisterFast(randomSeed);
    		
    		statsKeeper.searchStartingEvent(searchNumber);
       		BehaviorSearch.runProtocol(protocol, space, searchNumber, runOptions.numThreads, rng, 
       				batchRunner, statsKeeper) ;
       		statsKeeper.searchFinishedEvent();
    	}
  		
    	statsKeeper.allSearchesFinishedEvent();

    	try {
			batchRunner.dispose();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void runProtocol(SearchProtocolInfo protocol, SearchSpace space, int searchIDNumber, int numEvaluationThreads,
			MersenneTwisterFast rng, BatchRunner batchRunner, SearchProgressStatsKeeper statsKeeper)
			throws SearchParameterException, BehaviorSearchException, InterruptedException {		
		
        SearchMethod searcher = SearchMethodLoader.createFromName(protocol.searchAlgorithmInfo.searchMethodType);

        //if the search method in the protocol is missing some parameters, fill them in with defaults
        HashMap<String, String> defaultParams = searcher.getSearchParams();
        HashMap<String, String> searchParams = protocol.searchAlgorithmInfo.searchMethodParams;
        for (String key : defaultParams.keySet())
        {
        	if (!searchParams.containsKey(key))
        	{
        		searchParams.put(key, defaultParams.get(key));
        	}
        }        
		searcher.setSearchParams(searchParams);

		ObjectiveEvaluator ffun;
		if (protocol.modelDCInfo.fitnessSamplingReplications == 0 && !searcher.supportsAdaptiveSampling())
		{
			throw new BehaviorSearchException("Error: " + searcher.getName() + " does not support adaptive fitness sampling!");
        }
		else
		{
			ffun = new ObjectiveEvaluator(protocol.objectives);
		}

		SearchManager manager = new SearchManager(searchIDNumber, batchRunner, protocol, ffun, statsKeeper);
		        	
		ChromosomeFactory cFactory = ChromosomeTypeLoader.createFromName(protocol.searchAlgorithmInfo.chromosomeType);

		searcher.search( space , cFactory, protocol, manager, rng );			
	
	}
	


	public static class RunOptions implements Cloneable {
		@Option(name="-p",aliases={"--protocol"},required=true,usage="file (.bsearch) from which to load the search experiment protocol.")
		public String protocolFilename;
		
		@Option(name="-o",aliases={"--output"},required=true,usage="output filename STEM: will create files named STEM.xxxx.csv")
		public String outputStem;
		
		@Option(name="-t",aliases={"--threads"},usage="number of simultaneous threads to run the search with (defaults to the number of processors available).")
		public int numThreads = Runtime.getRuntime().availableProcessors();

		@Option(name="-n",aliases={"--numsearches"},usage="number of times to repeat the search (default 1)")
		public int numSearches = 1;

		@Option(name="-f",aliases={"--firstsearchnum"},usage="searches will be numbered starting at this index (only affects search # column in the output data)")
		public int firstSearchNumber = 1;
				
		@Option(name="-r",aliases={"--randomseed"},usage="random seed to start the first search (additional searches will be seeded with following consecutive numbers)")
		// if none specified, choose a random integer to start with.
		public Integer randomSeed = (Integer) new MersenneTwisterFast().nextInt();  

		@Option(name="-q",aliases={"--quiet"},usage="suppress printing progress to stdout")
		boolean quiet = false;
		
		@Option(name="-b", aliases={"--brief-output"},usage="shorthand flag for suppressing model-run-history and objective-function-history output, since these are the largest output files.")
		public	boolean briefOutput = false;

		@Option(name="--verbose-model-output", usage="enables logging of all the raw model measurements (EVERY model step that gets recorded).  This is likely too much data for most use cases, but could be useful in some situations, or for debugging.")
		public	boolean verboseModelOutput = false;

		@Option(name="--suppress-model-run-history",usage="don't create the .modelRuns.csv file")
		public boolean suppressModelRunHistory = false;

		@Option(name="--suppress-objective-function-history",usage="don't create the .objectiveHistory.csv file")
		public boolean suppressObjectiveFunctionHistory = false;

		@Option(name="--suppress-best-history",usage="don't create the .bestHistory.csv file")
		public boolean suppressBestHistory = false;

		@Option(name="--suppress-search-bests",usage="don't create the .searchBests.csv file")
		public boolean suppressSearchBests = false;

		@Option(name="--suppress-netlogo-shortcut",usage="don't create output column with the quick copy/paste text for recreating the parameter settings in NetLogo")
		public boolean suppressNetLogoCommandCenterColumn = false;

		@Option(name="-v", aliases={"--version"},usage="print version number and exit")
		public boolean printVersion = false;

    @Option(name="--override-parameter-spec",usage="override the parameter spec/range given in the .bsearch file, using usual syntax (e.g. [\"population\" 100 200 400] (NOTE: you may need to \\ escape the quotes in your shell)", multiValued=true)
		List<String> overrideParameters = new java.util.LinkedList<String>();

		//@Argument(usage="any number of arguments...")
		//List<String> arguments;
		
		@Override
		public RunOptions clone()
		{		
			try {
				return (RunOptions) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError();  // won't happen, since class implements Cloneable
			}
		}
	}
	
	public static void main(String[] args) {
		RunOptions runOptions = new RunOptions();
		CmdLineParser parser = new CmdLineParser(runOptions);
		
		try {
			parser.parseArgument(args);
			
		} catch( CmdLineException e ) {
			if (runOptions.printVersion)
			{
				System.out.println("BehaviorSearch v" + GeneralUtils.getVersionString());
				System.exit(0);
			}
			if (args.length == 0)
			{
				System.err.println("BehaviorSearch v" + GeneralUtils.getVersionString() + "\n");
			}
			else {
				System.err.println(e.getMessage());
			}
			String scriptName = "behaviorsearch.sh";
			if (GeneralUtils.isOSWindows())
			{
				scriptName = "behaviorsearch.bat";
			}
			System.err.println(scriptName + " [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: " + scriptName + " -p myexperiment.bsearch -o myoutput --threads 1 -n 5");
			System.exit(1);
		}
		try {
			runWithOptions(runOptions);
        }
        catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        finally {
        	try {
				NLogoUtils.fullyShutDownNetLogoLink();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
	}
	
	public static void runWithOptions(RunOptions runOptions) throws IOException, SAXException, BehaviorSearchException, InterruptedException, SearchParameterException
	{
		runWithOptions(runOptions,null,null);
	}
	/**
	 * Runs BehaviorSearch with the specified options (command line options, or could be specified through the GUI) 
	 * @param runOptions - Options for running BehaviorSearch  
	 * @param protocol - SearchProtocol to use for the search.  If null, then load the protocol from runOptions.protocolFilename.
	 * @param additionalListeners - a list of additional listeners that should receive events relating to the search progress.   
	 * @throws IOException
	 * @throws SAXException
	 * @throws BehaviorSearchException
	 * @throws InterruptedException
	 * @throws SearchParameterException
	 */
	public static void runWithOptions(RunOptions runOptions, SearchProtocolInfo protocol, List<ResultListener> additionalListeners) throws IOException, SAXException, BehaviorSearchException, InterruptedException, SearchParameterException
	{
		runOptions = (RunOptions) runOptions.clone(); // so we don't cause any side-effects to the parameter
		if (protocol == null)
		{
			runOptions.protocolFilename = GeneralUtils.attemptResolvePathFromStartupFolder(runOptions.protocolFilename);
			runOptions.outputStem = GeneralUtils.attemptResolvePathFromStartupFolder(runOptions.outputStem);
			protocol = SearchProtocolInfo.loadOldXMLBasedFile(runOptions.protocolFilename);
			//protocol = SearchProtocolInfo.loadFromFile(runOptions.protocolFilename + "2");
		}
		GeneralUtils.updateProtocolFolder(runOptions.protocolFilename);
        
		// allow users to override parameter spec ranges from the command line...
		for (String override : runOptions.overrideParameters)
		{
			ParameterSpec newSpec = ParameterSpec.fromString(override);
			boolean foundReplacement = false;
			
			for (int i = 0; i < protocol.paramSpecStrings.size(); i++) {
				ParameterSpec spec = ParameterSpec.fromString(protocol.paramSpecStrings.get(i));
				if (spec.getParameterName().equals(newSpec.getParameterName())) {
					protocol.paramSpecStrings.remove(i);
					protocol.paramSpecStrings.add(i, override);
					foundReplacement= true;
				}
			}
			if (!foundReplacement) {
				protocol.paramSpecStrings.add(override);
			}
		}
	
    	if (runOptions.briefOutput)
    	{
    		runOptions.suppressModelRunHistory = true;
    		runOptions.suppressObjectiveFunctionHistory = true;
    	}

        ArrayList<ResultListener> listeners = new ArrayList<ResultListener>();
    	listeners.add(new CSVLoggerListener(protocol, runOptions));
    	

    	if (!runOptions.quiet)
    	{
    		listeners.add(new ConsoleProgressListener(protocol.searchAlgorithmInfo.evaluationLimit, System.out));
    	}
    	if (additionalListeners != null)
    	{
    		listeners.addAll(additionalListeners);
    	}

    	BehaviorSearch.runMultipleSearches(protocol, listeners, runOptions );
		
	}
}
