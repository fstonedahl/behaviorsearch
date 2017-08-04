package bsearch.util;

import java.io.IOException;

import org.xml.sax.SAXException;

import bsearch.datamodel.SearchProtocolInfo;

public class TempProtocolFileFormatUpdater {

	public static void main(String[] args) throws IOException, SAXException {
		String[] filenames = new String[] {"test/Tester.bsearch", "test/TesterMOEA.bsearch", "test/TesterNoisy_RS.bsearch",
				"test/TesterSuperRandom.bsearch", "test/TesterGANoCache.bsearch", "test/TesterNoisy.bsearch", "test/TesterSA_Deriv.bsearch",
				"test/MiniFireFastOrSlowRunningTime.bsearch", "test/MiniFireOverTime.bsearch", "test/MiniFireVariance.bsearch",
				"test/TesterCombineMin.bsearch",
				"examples/Example_Fire_Burn_Variance.bsearch", "examples/Example_Fire_Derivative.bsearch",
				"examples/Example_Flocking_Convergence.bsearch"
				} ;

		for (String fname : filenames) {
			fname = GeneralUtils.attemptResolvePathFromBSearchRoot(fname);
			GeneralUtils.updateProtocolFolder(fname);
			SearchProtocolInfo protocol = SearchProtocolInfo.loadOldXMLBasedFile( fname ) ;
			protocol.save(fname.replace(".bsearch", ".bsearch2").replace(".xml", ".json"));
		}


	}

}
