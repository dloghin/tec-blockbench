#!/usr/bin/python
#
# Get total energy in Wh (Watt-hour) and J (Joules).
# Get average and peak power from Yokogawa WT210 power meter logs.
#
# Copyright (c) 2013 Dumitrel Loghin
#

import sys, glob, re, math
from array import *

# drop some lines from power logs
drop = 1
drop_lines_th = 2

def get_power_from_file(file):
	try:
		infile = open(file)
		energy = 0.0
		en1 = -1.0
		en2 = -1.0
		pmax = 0.0
		pavg = 0.0
		n = 0
		vals = array('f');
		l = 0	# no. of lines
		for line in infile:
			tokens = re.split('\s+', line)
			if len(tokens) > 2 and tokens[0] == "W":
				l += 1
				if drop and l <= drop_lines_th:
					continue
				try:
					pwr =  float(tokens[2])
					pmax = max(pmax, pwr)
					pavg += float(pwr)
					n = n + 1
				except ValueError:
					pwr = 0.0 # ignore bad value
			if len(tokens) > 3 and (tokens[0] == "HzV1N" or tokens[0] == "HzV1O"):
				if (en1 == -1.0):
					try:
						en1 = float(tokens[3])
					except ValueError:
						en1 = -1.0
				else:
					try:
						en = float(tokens[3])
					except ValueError:
						en = 0.0 ; # ignore bad value
					if (en >= en2):
						en2 = en
		if n > 0:
			energy = en2 - en1
			pavg = pavg / n

		infile.close()
	except:
		energy = 0.0
		pmax = 0.0
		pavg = 0.0
		pass

	print energy, "Wh", (3600.0 * float(energy)), "J", pmax, "W", pavg, "W"

	return

def main(argv):
	if len(sys.argv) < 2:
		print 'Usage: ', sys.argv[0], ' <power_data_file>'
		return

	get_power_from_file(sys.argv[1])

if __name__ == "__main__":
	main(sys.argv[0:])
