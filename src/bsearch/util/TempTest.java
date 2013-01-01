package bsearch.util;

import java.io.FileNotFoundException;

import org.nlogo.util.MersenneTwisterFast;

//TODO: Remove this class sometime.  It is just for some quick code testing.
public class TempTest {

	/**
	 * @param args
	 */
	static String formatAxisLabelString(double d)
	{
		String s = Double.toString(d);
		if (s.endsWith( ".0" ))
			s = s.substring(0, s.length() - 2);
		System.out.println("*" + s);
		if (s.length() > 8)
		{
			s = String.format("%.3g", d);
		}
	
		return s.replace("e+0","E").replace("e+", "E");		
	}

	public static void main(String[] args) {
		//String fstr = "%.6g";
		
		try {
			SimpleDiGraph.readEdgeListFromFile("testgraph.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.exit(0);
		
		MersenneTwisterFast rng = new MersenneTwisterFast();
		double d = 0.99;
		for (int i = 0; i < 10000; i++)
		{
			double rand = rng.nextGaussian() * 0.05;
			System.out.println(d + " " + rand);
			d = (d + rand) % 1.0;
			if (d < 0.0)
			{
				System.err.println("negative!");
				System.exit(0);
			}
			else if (d >= 1.0)
			{
				System.err.println("> 1");
				System.exit(0);				
			}
		}
		System.out.println(formatAxisLabelString(-123.456789));
		System.out.println(formatAxisLabelString(-123.4));
		System.out.println(formatAxisLabelString(-123.0));
		System.out.println(formatAxisLabelString(-1234.0));
		System.out.println(formatAxisLabelString(-12345.0));
		System.out.println(formatAxisLabelString(-123456789.0));
		System.out.println(formatAxisLabelString(-123456789e105));
		System.out.println(formatAxisLabelString(12345678));
//		System.out.println(Double.toString(-123456789e105));
		
		/*		try {

			HeadlessWorkspace workspace = Utils.createWorkspace();
			workspace.open("Tester.nlogo");
			//workspace.compileForRun("to meow __stdout 1234 end");
//			workspace.compiler().
			
			workspace.command("meow");
			workspace.dispose();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CompilerException e) {
			e.printStackTrace();
		} catch (LogoException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
*/
	}

}
