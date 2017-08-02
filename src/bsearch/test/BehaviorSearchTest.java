package bsearch.test;

//TODO: Improve test coverage!  (It's pretty weak right now... :-/)

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.MersenneTwisterFast;
import org.nlogo.api.SimpleJobOwner;
import org.nlogo.api.Version$;
import org.nlogo.core.LogoList;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Procedure;
import org.xml.sax.SAXException;

import bsearch.algorithms.SearchMethod;
import bsearch.algorithms.SearchMethodLoader;
import bsearch.algorithms.SearchParameterException;
import bsearch.app.BehaviorSearch;
import bsearch.app.BehaviorSearchException;
import bsearch.app.BehaviorSearch.RunOptions;
import bsearch.datamodel.ModelDataCollectionInfo;
import bsearch.datamodel.SearchProtocolInfo;
import bsearch.nlogolink.SingleRunResult;
import bsearch.nlogolink.ModelRunSetupInfo;
import bsearch.nlogolink.ModelRunner;
import bsearch.nlogolink.NLogoUtils;
import bsearch.nlogolink.NetLogoLinkException;
import bsearch.representations.Chromosome;
import bsearch.representations.ChromosomeFactory;
import bsearch.representations.ChromosomeTypeLoader;
import bsearch.representations.GrayBinaryChromosome;
import bsearch.representations.StandardBinaryChromosome;
import bsearch.space.SearchSpace;
import bsearch.util.GeneralUtils;

public strictfp class BehaviorSearchTest
{
  private static final String DEFAULT_MODEL_PATH = "/home/forrest/apps/NetLogo 6.0.1/app/models/Sample Models/";

	// a main method to run it -- for convenience.
	public static void main( String... args )
	{
		org.junit.runner.JUnitCore.main( BehaviorSearchTest.class.getName() ) ;
	}

  public String sampleModelsPath() {
    String osName = System.getProperty("os.name");
    String netLogoVersion = Version$.MODULE$.version();
    if (osName.contains("Mac")) {
      return "/Applications/" + netLogoVersion + "/models/Sample Models/";
    } else if (osName.contains("Win")) {
      return "C:\\Program Files\\" + netLogoVersion + "/app/models/Sample Models/";
    } else {
      return DEFAULT_MODEL_PATH;
    }
  }


	@Test
	public void testModelRunner1() throws Exception
	{
		LinkedHashMap<String,String> resultReporters = new LinkedHashMap<>();
		resultReporters.put("M1", "burned-trees");
		LinkedHashMap<String,String> singleRunCondenserReporters = new LinkedHashMap<>();
		singleRunCondenserReporters.put("C1", "mean @{M1}");
		ModelDataCollectionInfo modelDCInfo = new ModelDataCollectionInfo(sampleModelsPath() + "Earth Science/Fire.nlogo", 100,
				"setup", "go", "burned-trees > 3000", "ticks mod 10 = 0 or ticks = 37", resultReporters, singleRunCondenserReporters);
    	ModelRunner runner = bsearch.nlogolink.ModelRunner.createModelRunnerForTesting(modelDCInfo,Arrays.asList("mean @{C1}"));

		LinkedHashMap<String,Object> params= new LinkedHashMap<String, Object>();
        params.put("density", 61.0);
        ModelRunSetupInfo runSetup = new ModelRunSetupInfo(0, params);

        SingleRunResult runResult = runner.doFullRun( runSetup );
        LogoList resultList= (LogoList) runResult.getRawMeasureData("M1");
        Assert.assertEquals(5, resultList.size()); // 5 because we only get to tick 37, due to the stop condition.
        Assert.assertEquals(3002.0, ((Double) resultList.get(resultList.size() - 1)).doubleValue(), 0.00001);
        Assert.assertEquals(37.0, runner.report( "ticks" ));
        
        Object condensedResult = runResult.getCondensedResultMap().get("C1");
        Assert.assertEquals(1653.2, (double) condensedResult, 0.00001);

        // Now do a second run with a different random seed
        ModelRunSetupInfo runSetup2 = new ModelRunSetupInfo(1, params);
        SingleRunResult runResult2 = runner.doFullRun( runSetup2 );
        LogoList resultList2= (LogoList) runResult2.getRawMeasureData("M1");
        Assert.assertEquals(6, resultList2.size()); // because we only get to tick 46, due to the stop condition.
        Assert.assertEquals(2687.0, ((Double) resultList2.get(resultList2.size() - 1)).doubleValue(), 0.00001);
        Assert.assertEquals(46.0, runner.report( "ticks" ));
        
        Object condensedResult2 = runResult2.getCondensedResultMap().get("C1");
        Assert.assertEquals(1655.6666666667, (double) condensedResult2, 0.00001);
        
        // finally, combine the two runs
        List<Object> combinedResults = runner.evaluateCombineReplicateReporters(Arrays.asList(runResult, runResult2));
        Assert.assertEquals((double) combinedResults.get(0), 1654.433333333, 0.00001);
        

        runner.dispose();
	}

	@Test
	public void testConstraintsTextGeneration() throws BehaviorSearchException, NetLogoLinkException
    {
		Assert.assertEquals(bsearch.nlogolink.NLogoUtils.getDefaultConstraintsText(sampleModelsPath() + "/Social Science/Ethnocentrism.nlogo").trim(),
		"[\"mutation-rate\" [0 0.001 1]]\n[\"death-rate\" [0 0.05 1]]\n[\"immigrants-per-day\" [0 1 100]]\n[\"initial-ptr\" [0 0.01 1]]\n[\"cost-of-giving\" [0 0.01 1]]\n[\"gain-of-receiving\" [0 0.01 1]]\n[\"immigrant-chance-cooperate-with-same\" [0 0.01 1]]\n[\"immigrant-chance-cooperate-with-different\" [0 0.01 1]]");
    }

	@Test
	public void testNetLogoSubstitutionEvaluation() throws BehaviorSearchException, NetLogoLinkException
    {
		LogoListBuilder builder1 = new LogoListBuilder(); builder1.addAll(Arrays.asList(1.0,2.0,3.0));
		LogoListBuilder builder2 = new LogoListBuilder(); builder2.addAll(Arrays.asList("Moose","cow",true));
		LogoListBuilder builder3 = new LogoListBuilder(); builder3.addAll(Arrays.asList(2.5,3.5,1.5));
		Object thing4 = "Elephant";

		HeadlessWorkspace workspace = NLogoUtils.createWorkspace();
		SimpleJobOwner testOwner = new SimpleJobOwner("TestOwner", workspace.mainRNG(), org.nlogo.core.AgentKindJ.Observer());
		LinkedHashMap<String,Object> varsWithValues = new LinkedHashMap<>();
		varsWithValues.put("MEASURE1", builder1.toLogoList());
		varsWithValues.put("MEASURE2", builder2.toLogoList());
		varsWithValues.put("MEASURE3", builder3.toLogoList());
		varsWithValues.put("MEASURE4", thing4);
		
		String[] varNames = varsWithValues.keySet().toArray(new String[0]);
		Object[] varValues = varsWithValues.values().toArray();
		String originalReporter = "MAX (MAP + @{MEASURE1} @{MEASURE3})";
		Procedure reporterProc = NLogoUtils.substituteVariablesAndCompile(originalReporter, varNames, workspace);
		Object result1 = NLogoUtils.evaluateNetLogoWithSubstitution(originalReporter,reporterProc,varNames,varValues,workspace,testOwner);
		
		Assert.assertEquals(5.5, result1);

		originalReporter = "length @{MEASURE4} - length @{MEASURE2}";
		reporterProc = NLogoUtils.substituteVariablesAndCompile(originalReporter, varNames, workspace);
		Object result2 = NLogoUtils.evaluateNetLogoWithSubstitution(originalReporter,reporterProc,varNames, varValues,workspace,testOwner);
		
		Assert.assertEquals(5.0, result2);
    }	


	@Ignore("not practical while switching from xml to json") @Test
	public void testSearchProtocol() throws IOException , SAXException
	{
		String FILENAME = GeneralUtils.attemptResolvePathFromBSearchRoot("test/TestProtocol.xml") ;
		SearchProtocolInfo sp = SearchProtocolInfo.loadOldXMLBasedFile( FILENAME ) ;
		// is the result the same as the original, ignoring all whitespace?
		String result = sp.toJSONString().replaceAll( "\\s" , "" ) ;
		String original  = bsearch.util.GeneralUtils.stringContentsOfFile( new java.io.File(FILENAME) ).replaceAll( "\\s" , "" ) ;
		//System.out.println( result ) ;
		//System.out.println( original ) ;
		//System.out.println( result.equals( original ) ) ;
		Assert.assertEquals(result, original);
	}

	@Test
	public void testSearchMethodLoader() throws BehaviorSearchException
	{
		// just make sure there are no errors...
    	for (String s: SearchMethodLoader.getAllSearchMethodNames())
    	{
    		SearchMethod searcher = SearchMethodLoader.createFromName(s);
    		searcher.getSearchParams().toString();
    	}
	}

	@Test
	public void testChromosomes() throws BehaviorSearchException
	{
		SearchSpace ss = new SearchSpace(Arrays.asList("[\"discrete1to4\" [1 1 4]]",
														"[\"continuous0to1.5\" [0.0 \"C\" 1.5]]",
														"[\"categorical\" \"apple\" \"banana\" \"cherry\"]",
														"[\"const\" 25]",
														"[\"discretedecimal\" [-1 0.17 2]]"));
		MersenneTwisterFast rng = new MersenneTwisterFast();

    	for (String chromoType: ChromosomeTypeLoader.getAllChromosomeTypes())
    	{
    		ChromosomeFactory factory = ChromosomeTypeLoader.createFromName(chromoType);
    		Chromosome cs[] = new Chromosome[] {factory.createChromosome(ss, rng), factory.createChromosome(ss, rng)};
			for (int i = 0; i < 1000; i++)
			{
				cs[0] = cs[0].mutate(0.20, rng);
				cs[1] = cs[1].crossoverWith(cs[0], rng)[0];

				for (Chromosome c: cs )
				{
					LinkedHashMap<String, Object> params = c.getParamSettings();
					double val1 = (Double)params.get("discrete1to4");
					double val2 = (Double)params.get("continuous0to1.5");
          // legitimate bug here - we have a logolist of strings when we're expecting a
          // single string, unclear what broke
					String val3 = (String) params.get("categorical");
					Object val4 = params.get("const");
					double val5 = (Double)params.get("discretedecimal");
					Assert.assertTrue(chromoType + " check val1 >= 1", val1 >= 1 );
					Assert.assertTrue(chromoType + " check val1 <= 4", val1 <= 4 );
					Assert.assertTrue(chromoType + " check val2 >= 0.0", val2 >= 0.0 );
					Assert.assertTrue(chromoType + " check val2 <= 1.5", val2 <= 1.5 );
					Assert.assertTrue(chromoType + " check val3 okay", val3.equals("apple") || val3.equals("banana") || val3.equals("cherry"));
					Assert.assertTrue(chromoType + " check val4 okay", val4.equals(new Double(25)));
					Assert.assertTrue(chromoType + " check val5 >= -1", val5 >= -1 );
					Assert.assertTrue(chromoType + " check val5 <= 2", val5 <= 2 );
				}
			}
			for (int i = 0; i < 10000; i++)
			{
				Chromosome c = factory.createChromosome(ss, rng);
				//System.out.println(chromoType + " i=" + i + " : " + c);
				Chromosome c2 = factory.createChromosome(ss, c.getParamSettings());
				Assert.assertEquals(chromoType + " check recreate Chromosomes from values:", c.getParamSettings(), c2.getParamSettings());
			}
    	}
	}

	@Test
	public void testBinaryConversion()
	{
		int numBits = 8;
		boolean[] bits = new boolean[numBits + 12];

		SearchSpace ss = new SearchSpace(Arrays.asList("[\"moo\" [1 1 4]]"));
		GrayBinaryChromosome bcgray = new GrayBinaryChromosome(ss, new MersenneTwisterFast() );
		StandardBinaryChromosome bcstd = new StandardBinaryChromosome(ss, new MersenneTwisterFast() );

		for (long i = 0; i < (1L << numBits); i ++)
		{
			bcgray.binaryEncode(i, bits, 5, numBits);
			long dec = bcgray.binaryDecode(bits, 5, numBits);
			Assert.assertEquals(i, dec);
			bcstd.binaryEncode(i, bits, 5, numBits);
			dec = bcstd.binaryDecode(bits, 5, numBits);
//			if (i != dec)
//			{
//				System.out.println("i = " + i + ":  " + BinaryChromosome.toBinaryString(bits) + "   decode=" + dec);
//			}
			Assert.assertEquals(i, dec);
		}
		numBits = 62 ;
		bits= new boolean[numBits + 3];
		for (long i = 0; i < (1L << numBits); i += 15037093017331L)
		{
			bcgray.binaryEncode(i, bits, 2, numBits);
			long dec = bcgray.binaryDecode(bits, 2, numBits);
			Assert.assertEquals(i, dec);
			bcstd.binaryEncode(i, bits, 2, numBits);
			dec = bcstd.binaryDecode(bits, 2, numBits);
			Assert.assertEquals(i, dec);
		}
	}


	public static <T> String join(final Iterable<T> objs, final String delimiter) {
	    java.util.Iterator<T> iter = objs.iterator();
	    if (!iter.hasNext())
	        return "";
	    StringBuffer buffer = new StringBuffer(String.valueOf(iter.next()));
	    while (iter.hasNext())
	        buffer.append(delimiter).append(String.valueOf(iter.next()));
	    return buffer.toString();
	}

	@Test
	public void testConsistentOutputResults() throws IOException , SAXException, SearchParameterException, BehaviorSearchException, InterruptedException, CmdLineException
	{
	    Path tmpDirectory = Paths.get("test/tmp");
	    Files.createDirectories(tmpDirectory);
		LinkedHashMap<String,String> scenarios = new LinkedHashMap<String,String>();
		scenarios.put("TesterSuperRandom","-p test/TesterSuperRandom.bsearch -o test/tmp/TesterSuperRandom -t 7 -n 2 --randomseed 123 --quiet");
		scenarios.put("Tester1","-p test/Tester.bsearch -o test/tmp/Tester1 -t 1 -n 2 -f 3 --randomseed 1234 --quiet");
		scenarios.put("Tester2","-p test/Tester.bsearch -o test/tmp/Tester2 -t 2 -n 3 -f 10 --randomseed 99 --quiet");
		scenarios.put("TesterNoisy","-p test/TesterNoisy.bsearch -o test/tmp/TesterNoisy -t 2 -n 2 --randomseed 123 --quiet");
		scenarios.put("TesterGANoCache","-p test/TesterGANoCache.bsearch -o test/tmp/TesterGANoCache -n 1 -t 2 --randomseed 99 --quiet");
		scenarios.put("TesterSA_Deriv","-p test/TesterSA_Deriv.bsearch -o test/tmp/TesterSA_Deriv -n 2 -t 1 --randomseed 67 --quiet");
//		scenarios.put("TesterNoisy_RS","-p test/TesterNoisy_RS.bsearch -o test/tmp/TesterNoisy_RS -t 1 -n 1 --randomseed 123 --quiet");
		scenarios.put("TesterNoisy_RS","-p test/TesterNoisy_RS.bsearch -o test/tmp/TesterNoisy_RS -t 5 -n 1 --randomseed 123 --quiet");
		scenarios.put("TesterCombineMin","-p test/TesterCombineMin.bsearch -o test/tmp/TesterCombineMin -t 2 -n 1 --randomseed 1234 --quiet");
		scenarios.put("MiniFireVariance","-p test/MiniFireVariance.bsearch -o test/tmp/MiniFireVariance -t 1 -n 1 --randomseed 1 --quiet");
		scenarios.put("MiniFireOverTime","-p test/MiniFireOverTime.bsearch -o test/tmp/MiniFireOverTime -t 1 -n 1 --randomseed 1 --quiet");
		
		List<String> outputExtensions = Arrays.asList(//".searchConfig.xml", //TODO: switch to check JSON?
				".modelRunHistory.csv", 
				".objectiveFunctionHistory.csv",  ".bestHistory.csv", 
				".finalBests.csv", ".finalCheckedBests.csv"); 
		
		List<String> failedList = new ArrayList<String>();
		for (String key: scenarios.keySet())
		{
        	RunOptions clOptions = new RunOptions();
        	CmdLineParser parser = new CmdLineParser(clOptions);        	
        	parser.parseArgument(scenarios.get(key).split("\\s"));
			BehaviorSearch.runWithOptions(clOptions);

			for (String extension: outputExtensions)
			{
				String checkFileName = GeneralUtils.attemptResolvePathFromBSearchRoot("test/checks/" + key + extension);
				String testFileName = GeneralUtils.attemptResolvePathFromBSearchRoot("test/tmp/" + key + extension);
				File checkFile = new File(checkFileName);
				File testFile = new File(testFileName);
				String expected = checkFile.exists()?GeneralUtils.stringContentsOfFile(checkFile):"*no_file*";
				String result = testFile.exists()?GeneralUtils.stringContentsOfFile(testFile):"*no_file*";
				if (!result.equals(expected))
				{
					failedList.add(key + extension);
				}
			}
		}
		if (!failedList.isEmpty())
		{
			Assert.fail("Output files that differ: " + join(failedList, ", "));
		}
	}

}
