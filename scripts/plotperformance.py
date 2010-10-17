#!/usr/bin/env python

import glob
from optparse import OptionParser
from pylab import *

def latexEscape(s):
	if rcParams["text.usetex"]:
		return s.replace("_","\\_")
	else:
		return s
	
def uniq(alist):   # remove duplicates, preserve order
	set = {}
	return [set.setdefault(e,e) for e in alist if e not in set]

lastColor = None
def quickplot(xVals,yVals,stdErrVals,lab):
	global lastColor
	lab = latexEscape(lab)
	if stdErrVals != None:
		# for 90% confidence, use 1.65 instead of 1.96
		lines = errorbar(xVals,yVals,yerr=stdErrVals*1.96,label=lab)
	else:
		lines = plot(xVals,yVals,label=lab)
	lastColor = lines[0].get_color()

def quickplot2(xVals,yVals,stdErrVals,lab):
	global lastColor
	lab = latexEscape(lab)
	if stdErrVals != None:
		# for 90% confidence, use 1.65 instead of 1.96
		lines = errorbar(xVals,yVals,yerr=stdErrVals*1.96,label=lab,color=lastColor,linestyle="dotted") 
	else:
		lines = plot(xVals,yVals,label=lab,color=lastColor,linestyle="dotted")


def getShortNames(filenames):
	shortnames = list(filenames);
	if (len(set(shortnames)) == 1):
		return shortnames
	while len(set(x[0] for x in shortnames)) == 1:
		shortnames = [x[1:] for x in shortnames]
	while len(set(x[-1] for x in shortnames)) == 1:
		shortnames = [x[:-1] for x in shortnames]
	return shortnames


def main(outputFileName, inputPatterns, options):
	filenames = []
	for fPat in inputPatterns:
		filenames.extend(glob.glob(fPat))
	filenames = uniq(filenames)

	shortnames = getShortNames(filenames)
	figure()
	
	numSearches = set()
	for (fname,shortname) in zip(filenames,shortnames):
		dat = csv2rec(fname)
		if (options.max == None):			
			dat = dat[options.min::options.interval]
		else:
			dat = dat[options.min:options.max:options.interval]
			
		numSearches.add(dat[0]['num_searches'])
			
		if (options.errorbars):
			stdErrVals = dat['stderr_fitness']
			if (options.checked and "stderr_checked_fitness" in dat.dtype.names):
				checkedStdErrVals = dat['stderr_checked_fitness']
		else:
			stdErrVals = None
			checkedStdErrVals = None

		quickplot(dat['evaluations'],dat['mean_fitness'],stdErrVals,shortname)
		
		if (options.checked and "mean_checked_fitness" in dat.dtype.names):
			quickplot2(dat['evaluations'],dat['mean_checked_fitness'],checkedStdErrVals,shortname+"*checked")
	
	if (options.ymin != None):
		ylim(ymin=options.ymin)
	if (options.ymax != None):
		ylim(ymax=options.ymax)
	
	xlabel("number of evaluations (ABM runs)")
	ylabel(latexEscape(options.ylabel))
	if (options.title == None):
		options.title = outputFileName
	title(latexEscape(options.title)) #, ha='right')
	numSearches = [str(x) for x in sorted(numSearches)]
	figtext(0.97,0.02, "[avg. of %s searches]"%(" or ".join(numSearches)), 
			fontsize=9, ha='right')
	#legend(loc = 'lower right')
	import matplotlib.font_manager
	prop = matplotlib.font_manager.FontProperties(size=10)
	legend(loc=options.legendloc, prop=prop)
	#axis(ymax=0.6)

	savefig(outputFileName, dpi=options.dpi)

	###########################

if __name__ == '__main__':
	parser = OptionParser()
	parser.set_usage(parser.get_prog_name() + " output_graph.png file1.performance.csv ...")
	parser.set_description("""Takes xxx.performance.csv files and creates a graphic plot of the data.
The output type is determined by the output_graph file extension (.png, .pdf, .eps, .svg, etc).
(Note filename wildcards are allowed for input files.)""")
	parser.add_option("-e", "--errorbars", action="store_true", dest="errorbars", help="include error bars (95% confidence interval)")
	parser.add_option("-c", "--checked", action="store_true", dest="checked", help="also include checked fitness values in the plot")
	parser.add_option("-i", "--interval", action="store", type="int", dest="interval", default=1, help="only plot every Nth row of the input data file.")
	parser.add_option("--min", action="store", type="int", dest="min", default=0, help="start plotting at the Nth row of the input data file.")
	parser.add_option("--max", action="store", type="int", dest="max", default=None, help="stop plotting at the Nth row of the input data file.")
	parser.add_option("--ymin", action="store", type="float", dest="ymin", default=None, help="min y-value for plot window")
	parser.add_option("--ymax", action="store", type="float", dest="ymax", default=None, help="max y-value for plot window")

#	parser.add_option("-o", action="append", type="choice", choices=["png","pdf","eps","svg"], dest="fileformat", help="type of output file to be created")
	parser.add_option("--dpi", action="store", type="int", dest="dpi", help="DPI to use when exporting graphics (applicable to raster formats, like PNG)")
	parser.add_option("--ylabel", action="store", dest="ylabel", default="fitness", help="ylabel for the graph")
	parser.add_option("--title", action="store", dest="title", help="title for the graph")
	parser.add_option("--legendloc", action="store", dest="legendloc", default="lower right", help="location of the legend (e.g. 'lower right', 'upper right')")
	
	options , args = parser.parse_args()
	
	if (len(args) < 2):
		parser.print_help()
		sys.exit(0)
	outputFileName = args[0]
	outExtension = outputFileName.rsplit('.')[-1]
	if (outExtension.upper() == "CSV"):
		print "ERROR: 1st argument should be the graphic file to create: e.g. .png, .pdf, .svg"
		parser.print_help()
		sys.exit(0)
		
	inputPatterns = args[1:]
	main(outputFileName, inputPatterns, options)




