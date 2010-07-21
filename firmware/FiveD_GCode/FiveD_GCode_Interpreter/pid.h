#ifndef PID_H
#define PID_H

#if MOTHERBOARD != 2

// PID definitions

#define TEMP_PID_INTEGRAL_DRIVE_MAX 110
#define TEMP_PID_PGAIN 25.0 //3.0 // 5.0
#define TEMP_PID_IGAIN 30.0 //1.0 // 0.1
#define TEMP_PID_DGAIN 15.0 // 100.0

class PIDcontrol
{
  
private:

  volatile int iState; // Integrator state
  volatile int dState; // Last position input
  bool doingBed;
  unsigned long previousTime; // ms
  unsigned long time;
  int dt;
  float pGain;
  float iGain;
  float dGain;
  int temp_dState;
  long temp_iState;
  float temp_iState_max;
  float temp_iState_min;
  int output;
  int error;
  float pTerm, iTerm, dTerm;
  byte heat_pin, temp_pin;
//  bool bedTable;
  int currentTemperature;
 
  void internalTemperature(short table[][2]); 
  
public:

  PIDcontrol(byte hp, byte tp, bool b);
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

