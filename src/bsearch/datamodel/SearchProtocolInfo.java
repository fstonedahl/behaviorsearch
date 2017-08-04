package bsearch.datamodel ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import bsearch.datamodel.ObjectiveFunctionInfo.OBJECTIVE_TYPE;
import bsearch.util.GeneralUtils;

public strictfp class SearchProtocolInfo
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	
	//TODO: Add a "documentation" field to store text/comments about the search protocol.
	public final double bsearchVersionNumber;
	public final String infoTab;
	public final ModelDataCollectionInfo modelDCInfo;
	
	public final List<String> paramSpecStrings;
	
	public final List<ObjectiveFunctionInfo> objectives;
	
	public SearchAlgorithmInfo searchAlgorithmInfo; 
	
	
	@SuppressWarnings("unused") // used by Gson, NOTE: Set default values here for any new fields added in later versions!
	private SearchProtocolInfo() {
		super();
		this.bsearchVersionNumber = 0.72;  
		this.infoTab = "MOOSE";
		this.modelDCInfo = null;
		this.paramSpecStrings = new LinkedList<String>();;
		this.objectives = null;
		this.searchAlgorithmInfo = null;
	}
	/**
	 * A SearchProtocol stores the information about what is being searched, and how it should be done.
	 * These fields map to the BehaviorSearch GUI for designing an experiment/protocol in a 
	 * relatively straightforward fashion. 
	 */
	public SearchProtocolInfo(String modelFile, List<String> paramSpecStrings,
			String modelStepCommands, String modelSetupCommands, String modelStopCondition,
			int modelStepLimit, String modelMetricReporter, String modelMeasureIf, 
			String objectiveName,
			OBJECTIVE_TYPE objectiveType,
			int fitnessSamplingRepetitions,
			String fitnessCollecting,
			String fitnessCombineReplications,
			String fitnessDerivativeParameter,
			double fitnessDerivativeDelta,
			boolean fitnessDerivativeUseAbs,
			String searchMethodType,
			HashMap<String, String> searchMethodParams,
			String chromosomeType,
			boolean caching,
			int evaluationLimit, 
			int bestCheckingNumReplications) {
		super();
		this.bsearchVersionNumber = GeneralUtils.getVersionNumber();
		this.infoTab = "Write notes here...";
		
		LinkedHashMap<String,String> rawMeasures = new LinkedHashMap<>();
		rawMeasures.put("MEASURE1", modelMetricReporter);
		LinkedHashMap<String,String> condenserMeasures = new LinkedHashMap<>();
		condenserMeasures.put("CONDENSED1", fitnessCollecting);
		
		this.modelDCInfo = new ModelDataCollectionInfo(modelFile, modelStepLimit, modelSetupCommands, modelStepCommands,
				modelStopCondition, modelMeasureIf, rawMeasures, condenserMeasures,fitnessSamplingRepetitions,
				bestCheckingNumReplications);

		this.paramSpecStrings = paramSpecStrings;
		this.objectives = new ArrayList<>();
		this.objectives.add(new ObjectiveFunctionInfo(objectiveName,objectiveType, fitnessCombineReplications, fitnessDerivativeParameter, fitnessDerivativeDelta, fitnessDerivativeUseAbs));
		this.searchAlgorithmInfo = new SearchAlgorithmInfo(searchMethodType, searchMethodParams, chromosomeType, caching, evaluationLimit);
	}
	private static String loadOrGetDefault(XPath xpath, Document xmlDoc, String path, String defaultVal)
	{
		try {
			String s = (String) xpath.evaluate(path , xmlDoc , XPathConstants.STRING );
			if (s.equals(""))
			{ 
				return defaultVal; 
			}
			return s;
		} catch (XPathExpressionException e) {
			return defaultVal;
		}
	}
	private static int loadOrGetDefaultInt(XPath xpath, Document xmlDoc, String path, int defaultVal)
	{
		try {
			String s = (String) xpath.evaluate(path , xmlDoc , XPathConstants.STRING );
			if (s.equals(""))
			{ 
				return defaultVal; 
			}
			return Integer.valueOf(s.trim());
		} catch (XPathExpressionException e) {
			return defaultVal;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
	private static double loadOrGetDefaultDouble(XPath xpath, Document xmlDoc, String path, double defaultVal)
	{
		try {
			String s = (String) xpath.evaluate(path , xmlDoc , XPathConstants.STRING );
			if (s.equals(""))
			{ 
				return defaultVal; 
			}
			return Double.valueOf(s.trim());
		} catch (XPathExpressionException e) {
			return defaultVal;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	@Deprecated // keeping around for XML to JSON auto-conversion....?
	private SearchProtocolInfo(Document xmlDoc) throws XPathExpressionException
	{
		// Admittedly, XPath isn't the most efficient approach, but this code isn't performance critical, 
		// and I find XPath nice and readable. ~Forrest (10/28/2008)
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		bsearchVersionNumber = loadOrGetDefaultDouble(xpath,  xmlDoc, "/search/bsearchVersionNumber/text()" , 0.71); // 0.71 since that was the version before we added this XML tag
		infoTab = "Write notes here...";

		String modelFileName = loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelFile/text()" , "");
		String setupCommands = loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelSetupCommands/text()", "");
		String stepCommands = loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelStepCommands/text()" , "" );
		String stopCondition = loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelStopCondition/text()", "");
		int maxModelSteps = loadOrGetDefaultInt(xpath,  xmlDoc, "/search/modelInfo/modelStepLimit/text()", 100);
		String measureIfReporter= loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelMeasureIf/text()" , "true");
		LinkedHashMap<String,String> rawMeasures = new LinkedHashMap<>();		
		rawMeasures.put("MEASURE1", loadOrGetDefault(xpath,  xmlDoc, "/search/modelInfo/modelMetricReporter/text()" , ""));
		LinkedHashMap<String,String> condenserMeasures = new LinkedHashMap<>();
		String condenseCode = loadOrGetDefault(xpath,  xmlDoc, "/search/fitnessInfo/fitnessCollecting/text()" , "N/A" );
		if (Arrays.asList("MEAN_ACROSS_STEPS", "MEDIAN_ACROSS_STEPS", "MIN_ACROSS_STEPS", "MAX_ACROSS_STEPS", 
				"VARIANCE_ACROSS_STEPS","SUM_ACROSS_STEPS").contains(condenseCode)) {
			condenseCode = condenseCode.replace("_ACROSS_STEPS","").toLowerCase() + " @{MEASURE1}"; 	
		} else if (condenseCode.equals("AT_FINAL_STEP")) {
			condenseCode = "last @{MEASURE1}";
			measureIfReporter = ModelDataCollectionInfo.SPECIAL_MEASURE_IF_DONE_FLAG; 
		}

		condenserMeasures.put("CONDENSED1", condenseCode);

		int fitnessSamplingReplications = loadOrGetDefaultInt(xpath,  xmlDoc, "/search/fitnessInfo/fitnessSamplingReplications/text()" , 10);
		int bestCheckingNumReplications = loadOrGetDefaultInt(xpath,  xmlDoc, "/search/bestCheckingNumReplications/text()" , 0);

		this.modelDCInfo = new ModelDataCollectionInfo(modelFileName, maxModelSteps, setupCommands, stepCommands, stopCondition, measureIfReporter, 
				rawMeasures, condenserMeasures, fitnessSamplingReplications, bestCheckingNumReplications);
		
		
		boolean fitnessMinimized = Boolean.valueOf(loadOrGetDefault(xpath,  xmlDoc, "/search/fitnessInfo/fitnessMinimized/text()" , "false").trim()); 
		String fitnessCombineReplications = loadOrGetDefault(xpath, xmlDoc,"/search/fitnessInfo/fitnessCombineReplications/text()" , "MEAN");
		if (Arrays.asList("MEAN", "MEDIAN", "MIN", "MAX", "VARIANCE").contains(fitnessCombineReplications)) {
			fitnessCombineReplications = fitnessCombineReplications.toLowerCase() + " @{CONDENSED1}"; 	
		} else if (fitnessCombineReplications.equals("STDEV")) {
			fitnessCombineReplications = "standard-deviation @{CONDENSED1}";
		}
		
		String fitnessDerivativeParameter = loadOrGetDefault(xpath, xmlDoc,"/search/fitnessInfo/fitnessDerivative/@parameter" , ""); 
		double fitnessDerivativeDelta = loadOrGetDefaultDouble(xpath, xmlDoc,"/search/fitnessInfo/fitnessDerivative/@delta" , 0.0); 
		boolean fitnessDerivativeUseAbs = Boolean.valueOf(loadOrGetDefault(xpath,  xmlDoc, "/search/fitnessInfo/fitnessDerivative/@useabs" , "false").trim()); 
		this.objectives = new ArrayList<>();
		OBJECTIVE_TYPE objectiveType = fitnessMinimized? OBJECTIVE_TYPE.MINIMIZE : OBJECTIVE_TYPE.MAXIMIZE;
		this.objectives.add(new ObjectiveFunctionInfo("objective1", objectiveType, fitnessCombineReplications, fitnessDerivativeParameter, fitnessDerivativeDelta, fitnessDerivativeUseAbs));
		
		NodeList paramSpecNodes = (NodeList) xpath.evaluate("/search/searchSpace/paramSpec/text()" , xmlDoc , XPathConstants.NODESET );
		paramSpecStrings = new LinkedList<String>();
		for (int i = 0; i < paramSpecNodes.getLength(); i++)
		{
			Node n = paramSpecNodes.item( i );
			paramSpecStrings.add( n.getNodeValue().trim() );
		}
		
		String searchMethodType = loadOrGetDefault(xpath,  xmlDoc, "/search/searchMethod/@type" , "RandomSearch" ); 

		NodeList methodParamNodes = (NodeList) xpath.evaluate("/search/searchMethod/searchMethodParameter" , xmlDoc , XPathConstants.NODESET );
		LinkedHashMap<String,String> searchMethodParams = new LinkedHashMap<String,String>();
		for (int i = 0; i < methodParamNodes.getLength(); i++)
		{
			Element n = (Element) methodParamNodes.item( i );
			searchMethodParams.put( n.getAttribute( "name" ) , n.getAttribute("value") );
		}
		String chromosomeType = loadOrGetDefault(xpath,  xmlDoc, "/search/chromosomeRepresentation/@type" , "MixedTypeChromosome" );
		boolean caching = Boolean.valueOf(loadOrGetDefault(xpath,  xmlDoc, "/search/caching/text()" , "true"));
		int evaluationLimit = loadOrGetDefaultInt(xpath,  xmlDoc, "/search/evaluationLimit/text()" , 300);		
		
		this.searchAlgorithmInfo = new SearchAlgorithmInfo(searchMethodType, searchMethodParams, chromosomeType, caching, evaluationLimit);
		
	}
	
	public void save(String filename) throws IOException
	{
		FileWriter out = new FileWriter(filename);
		out.write(toJSONString());
		out.close();
	}
	
	public String toJSONString()
	{
		return GSON.toJson(this);
	}
	
	public static SearchProtocolInfo loadFromFile(String fullPathFilename) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		return loadFromFile(new File(fullPathFilename));
	}
	public static SearchProtocolInfo loadFromFile(File file) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		return loadFromJSONString(GeneralUtils.stringContentsOfFile(file));
	}
	public static SearchProtocolInfo loadFromJSONString(String json) {
		return GSON.fromJson(json, SearchProtocolInfo.class);
	}
	
	
	public static SearchProtocolInfo loadOldXMLBasedFile(String fullPathFilename) throws IOException, SAXException 
	{
		BufferedReader reader = new BufferedReader(new FileReader(new File(fullPathFilename)));
		try { return loadOldXML(new InputSource(reader)); } 
		finally { reader.close(); }		
	}
	public static SearchProtocolInfo loadOldXML(String xmlStr)
		throws java.io.IOException , SAXException
	{
		InputSource inputSource = new InputSource(
				new java.io.StringReader(//"<!DOCTYPE search SYSTEM \"behaviorsearch.dtd\">\n" +  
						xmlStr));
		return loadOldXML(inputSource);
	}
	private static SearchProtocolInfo loadOldXML(InputSource inputSource)
		throws java.io.IOException , SAXException
	{
		File resourcesDir = new File( GeneralUtils.attemptResolvePathFromBSearchRoot("resources/" ));
		inputSource.setSystemId(resourcesDir.toURI().toString() ) ;

		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance() ;
		Document doc = null ;
		try
		{
			// To validate, we may need to help it find the DTD file...
			// inputSource.setSystemId( getClass().getResource( "/" ).toString() ) ;
			fac = DocumentBuilderFactory.newInstance() ;
			//TODO: Perhaps turn xml DTD validation back on?  Or is that too strict?  Is the DTD up to date?
			//fac.setValidating( true ) ;
			DocumentBuilder builder = fac.newDocumentBuilder() ;
			// Choke on any error or warning during parsing, not just fatal errors.
			builder.setErrorHandler( new ErrorHandler()
			{
				public void error( SAXParseException ex ) throws SAXException
				{	throw ex ;	}

				public void fatalError( SAXParseException ex ) throws SAXException
				{  	throw ex ;	}

				public void warning( SAXParseException ex ) throws SAXException
				{	throw ex ;	}
			} ) ;
			doc = builder.parse( inputSource ) ;
			return new SearchProtocolInfo(doc);
		}
		catch( ParserConfigurationException ex )
		{
			ex.printStackTrace() ;
			System.exit( 1 ) ;
		}
		catch( XPathExpressionException e )
		{
			e.printStackTrace();
			System.exit( 1 ) ;
		}
		throw new IllegalStateException ("Unreachable code.");
	}


}
