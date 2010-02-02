#ifndef PARAMETERS_H
#define PARAMETERS_H

/*
 *  This is the configuration file for the RepRap Motherboard microcontroller.
 *  Set values in it to match your RepRap machine.
 *  
 *  Lines in here with a 
 *
 *        // *RO
 *
 *  Comment at the end (read-only) should probably only be changed if you really
 *  know what you are doing...
 */

// Here are the Motherboard codes; set MOTHERBOARD to the right one
// A standard Mendel is MOTHERBOARD 2

// (Arduino: 0 - no longer in use)
// Sanguino or RepRap Motherboard with direct drive extruders: 1
// RepRap Motherboard with RS485 extruders: 2
// Arduino Mega: 3

#define MOTHERBOARD 2

// Comment out the next line if you are running a Darwin

#define MENDEL 1

// The speed at which to talk with the host computer; default is 19200

#define HOST_BAUD 19200 // *RO

// The number of real extruders

#define EXTRUDER_COUNT 1

// Set 1s where you have endstops; 0s where you don't
// Both Darwin and Mendel have MIN endstops, but not MAX ones.

#define ENDSTOPS_MIN_ENABLED 1
#define ENDSTOPS_MAX_ENABLED 0

// The width of Henry VIII's thumb (or something).

#define INCHES_TO_MM 25.4 // *RO

// The number of mm below which distances are insignificant (one tenth the
// resolution of the machine is the default value).

#define SMALL_DISTANCE 0.01 // *RO

// Useful to have its square

#define SMALL_DISTANCE2 (SMALL_DISTANCE*SMALL_DISTANCE) // *RO

#ifdef MENDEL

// define the XYZ parameters of Mendel

#define X_STEPS_PER_MM   10.047
#define X_STEPS_PER_INCH (X_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_X_DIR 0

#define Y_STEPS_PER_MM   10.047
#define Y_STEPS_PER_INCH (Y_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_Y_DIR 0

#define Z_STEPS_PER_MM   833.398
#define Z_STEPS_PER_INCH (Z_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_Z_DIR 0

#else

// This is for Darwin.

#define X_STEPS_PER_MM   7.99735
#define X_STEPS_PER_INCH (X_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_X_DIR 0

#define Y_STEPS_PER_MM   7.99735
#define Y_STEPS_PER_INCH (Y_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_Y_DIR 0

#define Z_STEPS_PER_MM   320
#define Z_STEPS_PER_INCH (Z_STEPS_PER_MM*INCHES_TO_MM) // *RO
#define INVERT_Z_DIR 0

#endif

// For when we have a stepper-driven extruder
// E_STEPS_PER_MM is the number of steps needed to 
// extrude 1mm out of the nozzle.

#define E_STEPS_PER_MM   0.706   // NEMA 17 extruder 5mm diameter drive - empirically adjusted

//#define E_STEPS_PER_MM   2.2       // NEMA 14 geared extruder 8mm diameter drive

#define E_STEPS_PER_INCH (E_STEPS_PER_MM*INCHES_TO_MM) // *RO

//our maximum feedrates
#define FAST_XY_FEEDRATE 3000.0
#define FAST_Z_FEEDRATE  50.0

// Data for acceleration calculations
// Comment out the next line to turn accelerations off
#define ACCELERATION_ON
#define SLOW_XY_FEEDRATE 1000.0 // Speed from which to start accelerating
#define SLOW_Z_FEEDRATE 20

// Set to 1 if enable pins are inverting
// For RepRap stepper boards version 1.x the enable pins are *not* inverting.
// For RepRap stepper boards version 2.x and above the enable pins are inverting.
#define INVERT_ENABLE_PINS 1

#if INVERT_ENABLE_PINS == 1
#define ENABLE_ON LOW
#else
#define ENABLE_ON HIGH
#endif

// Set these to 1 to disable an axis when it's not being used,
// and for the extruder.  Usually only Z is disabled when not in
// use.  You will probably find that disabling the others (i.e.
// powering down the steppers that drive them) when the ends of
// movements are reached causes poor-quality builds.  (Inertia
// causes overshoot if the motors are not left powered up.)

#define DISABLE_X 0
#define DISABLE_Y 0
#define DISABLE_Z 1
#define DISABLE_E 0

// Set to one if the axis opto-sensor outputs inverting (ie: 1 means open, 0 means closed)
// RepRap opto endstops with H21LOI sensors are not inverting; ones with H21LOB
// are inverting.

#define ENDSTOPS_INVERTING 1

// The number of 5-second intervals to wait at the target temperature for things to stabilise.
// Too short, and the extruder will jam as only part of it will be hot enough.
// Too long and the melt will extend too far up the insulating tube.
// Default value: 10

#define WAIT_AT_TEMPERATURE 10

//our command string length

#define COMMAND_SIZE 128 // *RO

// The size of the movement buffer

#define BUFFER_SIZE 4 // *RO

// Number of microseconds between timer interrupts when no movement
// is happening

#define DEFAULT_TICK (long)1000 // *RO

// What delay() value to use when waiting for things to free up in milliseconds

#define WAITING_DELAY 1 // *RO

#if MOTHERBOARD > 1

#define MY_NAME 'H'           // Byte representing the name of this device
#define E0_NAME '0'           // Byte representing the name of extruder 0
#define E1_NAME '1'           // Byte representing the name of extruder 1

#define RS485_MASTER  1       // *RO

#endif

//******************************************************************************

// You probably only want to edit things below this line if you really really
// know what you are doing...

extern char debugstring[];

void delayMicrosecondsInterruptible(unsigned int us);

// Inline interrupt control functions

inline void enableTimerInterrupt() 
{
   TIMSK1 |= (1<<OCIE1A);
}
	
inline void disableTimerInterrupt() 
{
     TIMSK1 &= ~(1<<OCIE1A);
}
        
inline void setTimerCeiling(unsigned int c) 
{
    OCR1A = c;
}

inline void resetTimer()
{
  TCNT2 = 0;
}

#endif

//*************************************************************************

#if 0
// Green machine:
//#define ENDSTOPS_INVERTING 0

// parameters for the Bath U. mendel prototype
#define X_STEPS_PER_MM   13.333333
#define X_STEPS_PER_INCH (X_STEPS_PER_MM*INCHES_TO_MM)
#define INVERT_X_DIR 0

#define Y_STEPS_PER_MM   13.333333
#define Y_STEPS_PER_INCH (Y_STEPS_PER_MM*INCHES_TO_MM)
#define INVERT_Y_DIR 0

// Green machine:
#define Z_STEPS_PER_MM   944.88
// Fat Z cog machine:
//#define Z_STEPS_PER_MM   558.864
// Standard Mendel:
//#define Z_STEPS_PER_MM   833.398
#define Z_STEPS_PER_INCH (Z_STEPS_PER_MM*INCHES_TO_MM)
#define INVERT_Z_DIR 0

#endif
