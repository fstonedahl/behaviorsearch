package bsearch.representations ;

import java.util.LinkedHashMap ;

import org.nlogo.util.MersenneTwisterFast ;

import bsearch.space.SearchSpace;

//TODO: make a CrossNet chromosome sometime? ~Forrest 3/28/09

/**
 * NOTE:
 * Implementing classes should also override equals(Object) and hashCode(),
 * to correspond with genotypic equality for the Chromosome.
 * 
 * Also all Chromosome implementations should be immutable (from an outside perspective).
 * 
 */
public strictfp interface Chromosome extends Cloneable
{

	/**
	 * @return a mapping of variable names (of model parameters) to values.
	 */
	public abstract LinkedHashMap<String , Object> getParamSettings() ;

	public abstract Chromosome clone() ;

	/** Note, this should return a new mutated Chromosome.  It SHOULD NOT modify the calling Chromosome. */
	public abstract Chromosome mutate( double mutRate , MersenneTwisterFast rng ) ;
	
	public abstract Chromosome[] crossoverWith(Chromosome other, MersenneTwisterFast rng ) ;
	
	public abstract SearchSpace getSearchSpace();

}