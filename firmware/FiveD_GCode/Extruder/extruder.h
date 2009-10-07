#ifndef EXTRUDER_H
#define EXTRUDER_H

#define REPLY_LENGTH 20

#define WAIT_T 'W'        // wait_for_temperature();
#define VALVE 'V'         // valve_set(bool open, int dTime);
#define DIRECTION 'D'     // set_direction(bool direction);
#define COOL 'C'          // set_cooler(byte e_speed);
#define SET_T 'T'         // set_temperature(int temp);
#define GET_T 't'         // get_temperature();
#define STEP 'S'          // step();
#define ENABLE 'E'        // enableStep();
#define DISABLE 'e'       // disableStep();
#define PREAD 'R'         // read the pot voltage
#define SPWM 'M'          // Set the motor PWM
#define PING 'P'          // Just acknowledge

class extruder
{

public:
   extruder();

   char* processCommand(char command[]);
   
   void manage();
  
private:

   byte coilPosition;// Stepper position between 0 and 7 inclusive
   byte pwmValue;    // PWM to the motor
   byte stp;         // Tracks the step signal
   int  temp;        // Target temperature in C
   int  t;           // Current temperature in C
   int  manageCount; // Timing in the manage function
   bool forward;     // Extrude direction
   byte blink;       // For the LED
   char reply[REPLY_LENGTH];  // For sending messages back

   void waitForTemperature();
   void slowManage();
   int internalTemperature();
   void valveSet(bool open);
   void setDirection(bool direction);
   void setCooler(byte e_speed);
   void setTemperature(int t);
   int getTemperature();
   void sStep();
   void enableStep();
   void disableStep();
   int potVoltage();
   void setPWM(int p); 
};

#endif

