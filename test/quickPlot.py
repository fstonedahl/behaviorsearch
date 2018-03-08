#!/usr/bin/env python3

import pandas as pd
import glob
from pylab import *

def quickPlot(fnameStem):
    #~ dat = pd.read_csv(fnameStem+".objectiveHistory.csv")
    dat = pd.read_csv(fnameStem)

    dat.plot.scatter('[model-run-count]','objective1',c='[search-number]',cmap='cool',edgecolors='face')
    if ('objective1-rechecked' in dat):
        dat.plot.scatter('[model-run-count]','objective1-rechecked')
    
    #~ dat.plot.scatter('[model-run-count]','objective2')
    #~ if ('objective2-rechecked' in dat):
        #~ dat.plot.scatter('[model-run-count]','objective2-rechecked')
    
    #~ dat.plot.scatter('[model-run-count]','a')
    #~ dat.plot.scatter('[model-run-count]','b')
    #~ dat.plot.scatter('[model-run-count]','c')
    
    if ('objective2' in dat):
        dat.plot.scatter('objective1','objective2', c='[model-run-count]', cmap='cool',edgecolors='face')
    #~ dat.plot.scatter('objective0-rechecked','objective1-rechecked')
    title(fnameStem)
    show()
    
    #~ datObj = pd.read_csv(fnameStem+".bestHistory.csv")
    #~ dat.plot.scatter('[model-run-count]','objective1')


#~ for fname in glob.glob('tmp/*.searchConfig.json'):
    #~ quickPlot(fname.replace('.searchConfig.json',''))
    
#~ for fname in glob.glob('tmp/Tester2*.bestHistory.csv'):
    #~ quickPlot(fname)

import sys
if len(sys.argv) > 1:
    for fname in sys.argv[1:]:
        quickPlot(fname)
