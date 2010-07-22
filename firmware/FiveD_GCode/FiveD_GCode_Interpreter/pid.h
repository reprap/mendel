#ifndef PID_H
#define PID_H

// Based on the excellent Wikipedia PID control article.
// See http://en.wikipedia.org/wiki/PID_controller

#if MOTHERBOARD != 2

// PID gains.  These are about right for a brass extruder about 8 mm 
// in diameter and 30 mm long heated by a 6 ohm coil with a 12v supply.

#define TEMP_PID_PGAIN 2
#define TEMP_PID_IGAIN 0.07
#define TEMP_PID_DGAIN 1

class PIDcontrol
{
  
private:

  bool doingBed;
  unsigned long previousTime; // ms
  unsigned long time;
  float previousError;
  float integral;
  float pGain;
  float iGain;
  float dGain;
  byte heat_pin, temp_pin;
  int currentTemperature;
 
  void internalTemperature(short table[][2]); 
  
public:

  PIDcontrol(byte hp, byte tp, bool b);
  void reset();
  void pidCalculation(int target);
  void shutdown();
  int temperature();
  
};

inline int PIDcontrol::temperature() 
{ 
  return currentTemperature; 
}

#endif
#endif

