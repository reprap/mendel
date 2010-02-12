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
#define UPFM 'U'          // Use the pot to control the motor
#define PING 'P'          // Just acknowledge

// PID definitions

#define TEMP_PID_INTEGRAL_DRIVE_MAX 110
#define TEMP_PID_PGAIN 5.0
#define TEMP_PID_IGAIN 0.1
#define TEMP_PID_DGAIN 100.0

//******************************************************************************************************

//our RS485 pins

#define RX_ENABLE_PIN 4 
#define TX_ENABLE_PIN 16  

// Pins to direct-drive the extruder stepper

#define E_STEP_PIN 10
#define E_DIR_PIN 9 

#ifdef MAX6675_THERMOCOUPLE
// I2C pins for the MAX 6675 temperature chip
 #define SO 18    // MISO
 #define SCK 19   // Serial Clock
 #define TC_0 17  // CS Pin of MAX6607
#else
 #define TEMP_PIN 3
#endif

// Control pins for the A3949 chips

#define H1D 7
#define H1E 5
#define H2D 8
#define H2E 6

// Analogue read of this pin gets the potentiometer setting

#define POT 0

// MOSFET drivers

#define OUTPUT_A 15
#define OUTPUT_B 11
#define OUTPUT_C 12

#define DEBUG_PIN 13

// The LED blink function

extern void blink(bool on);

// ******************************************************************************

class extruder
{

public:
   extruder();

   char* processCommand(char command[]);
   
   void manage();
  
private:

   byte coilPosition;// Stepper position between 0 and 7 inclusive
   byte pwmValue;    // PWM to the motor
   byte potVal;      // The averaged pot voltage
   byte potCount;    // Averaging counter
   int  potSum;      // For computing the pot average
   bool usePot;      // True to control the motor by the pot
   byte stp;         // Tracks the step signal
   int  targetTemperature;        // Target temperature in C
   int  currentTemperature;           // Current temperature in C
   int  manageCount; // Timing in the manage function
   bool forward;     // Extrude direction
   char reply[REPLY_LENGTH];  // For sending messages back
   
#ifdef PID_CONTROL

  volatile int iState; // Integrator state
  volatile int dState; // Last position input
  unsigned long previousTime; // ms
  float pGain;
  float iGain;
  float dGain;
  int temp_dState;
  long temp_iState;
  float temp_iState_max;
  float temp_iState_min;

  byte pidCalculation(int dt);

#endif

#ifdef PASTE_EXTRUDER

bool valveTimeCheck(int millisecs);
bool valveAlreadyRunning;
long valveEndTime;
void valveTurn(bool close);
bool valveAtEnd;
bool seenHighLow;
bool valveState;
void valveMonitor();
bool requiredValveState;

#endif

   void waitForTemperature();
   void slowManage();
   int internalTemperature();
   void valveSet(bool open);
   void setDirection(bool direction);
   void setCooler(byte e_speed);
   void setTemperature(int t);
   int getTemperature();
   void controlTemperature();
   void sStep();
   void enableStep();
   void disableStep();
   int potVoltage();
   void setPWM(int p);
   void usePotForMotor(); 
};


#endif

