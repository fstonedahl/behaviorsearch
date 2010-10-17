package bsearch.representations;

import org.nlogo.util.MersenneTwisterFast;

import bsearch.space.SearchSpace;

public strictfp interface ChromosomeFactory {

	public Chromosome createChromosome(SearchSpace searchSpace , MersenneTwisterFast rng);
	public abstract String getHTMLHelpText();
	
}
