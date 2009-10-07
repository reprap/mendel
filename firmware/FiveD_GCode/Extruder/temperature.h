#ifndef TEMPERATURE_H
#define TEMPERATURE_H


// How many temperature samples to take for an average.  each sample takes about 100 usecs.
#define TEMPERATURE_SAMPLES 3

// How accurately do we maintain the temperature?
#define HALF_DEAD_ZONE 5

// Thermistor lookup table for RepRap Temperature Sensor Boards 
// See this page:  
// http://dev.www.reprap.org/bin/view/Main/Thermistor
// for details of what goes in this table.

// Farnell code for this thermistor: 882-9586
// Made with createTemperatureLookup.py (http://svn.reprap.org/trunk/reprap/firmware/Arduino/utilities/createTemperatureLookup.py)
// ./createTemperatureLookup.py --r0=100000 --t0=25 --r1=0 --r2=4700 --beta=4066 --max-adc=1023
// r0: 100000
// t0: 25
// r1: 0
// r2: 4700
// beta: 4066
// max adc: 1023

#ifdef USE_THERMISTOR
#define NUMTEMPS 20
short temptable[NUMTEMPS][2] = {
   {1, 841},
   {54, 255},
   {107, 209},
   {160, 184},
   {213, 166},
   {266, 153},
   {319, 142},
   {372, 132},
   {425, 124},
   {478, 116},
   {531, 108},
   {584, 101},
   {637, 93},
   {690, 86},
   {743, 78},
   {796, 70},
   {849, 61},
   {902, 50},
   {955, 34},
   {1008, 3}
};

#endif
#endif
