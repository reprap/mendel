
// Class for controlling each extruder
//
// Adrian Bowyer 14 May 2009

#ifndef EXTRUDER_H
#define EXTRUDER_H



void manageAllExtruders();

void newExtruder(byte e);

/**********************************************************************************************

* Sanguino/RepRap Motherboard v 1.0

*/

#if MOTHERBOARD < 2

#define EXTRUDER_FORWARD true
#define EXTRUDER_REVERSE false

class extruder
{

     
public:

   extruder(byte md_pin, byte ms_pin, byte h_pin, byte f_pin, byte t_pin, byte vd_pin, byte ve_pin, byte se_pin);
   void waitForTemperature();
   void valveSet(bool open, int dTime);
   void setDirection(bool direction);
//   void set_speed(float es);
   void setCooler(byte e_speed);
   void setTemperature(int temp);
   int getTemperature();
   void manage();
// Interrupt setup and handling functions for stepper-driven extruders
   
//   void interrupt();
   void sStep();

   void enableStep();
   void disableStep();
   
private:

//these our the default values for the extruder.
    byte e_speed;
    int target_celsius;
    int max_celsius;
    byte heater_low;
    byte heater_high;
    byte heater_current;
    int extrude_step_count;

// These are used for temperature control    
    byte count ;
    int oldT, newT;
    
//this is for doing encoder based extruder control
//    int rpm;
//    long e_delay;
//    int error;
//    int last_extruder_error;
//    int error_delta;
    bool e_direction;
    bool valve_open;

// The pins we control
    byte motor_dir_pin, motor_speed_pin, heater_pin, fan_pin, temp_pin, valve_dir_pin, valve_en_pin, step_en_pin;
    
     byte wait_till_hot();
     //byte wait_till_cool();
     void temperatureError(); 
     int sampleTemperature();
   
};

inline void extruder::enableStep()
{
  if(step_en_pin < 0)
    return;
  digitalWrite(step_en_pin, ENABLE_ON); 
}

inline void extruder::disableStep()
{
  if(step_en_pin < 0)
    return;
#if DISABLE_E
  digitalWrite(step_en_pin, !ENABLE_ON);
#endif
}

inline void extruder::sStep()
{
   digitalWrite(motor_speed_pin, HIGH);
   delayMicrosecondsInterruptible(5); 
   digitalWrite(motor_speed_pin, LOW);
}


inline void extruder::setDirection(bool dir)
{
	e_direction = dir;
	digitalWrite(motor_dir_pin, e_direction);
}

inline void extruder::setCooler(byte sp)
{
  if(step_en_pin >= 0) // Step enable conflicts with the fan
    return;
  analogWrite(fan_pin, sp);
}

/**********************************************************************************************

* RepRap Motherboard v 1.x (x >= 1)
* Extruder is now on RS485

*/

#else


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

class extruder
{
  
public:
   extruder(char name);
   void waitForTemperature();
   void valveSet(bool open, int dTime);
   void setDirection(bool direction);
   void setCooler(byte e_speed);
   void setTemperature(int temp);
   int getTemperature();
   void manage();
   void sStep();
   void enableStep();
   void disableStep();
   int potVoltage();
   void setPWM(int p);
   void usePotForMotor();
   bool ping();
 
private:

   char my_name;
   int target_celsius;
   int count;
   int oldT, newT;
   char commandBuffer[RS485_BUF_LEN];
   char* reply;
   bool stp;

   void buildCommand(char c);   
   void buildCommand(char c, char v);
   void buildNumberCommand(char c, int v);
   void temperatureError();  
};

inline extruder::extruder(char name)
{
  my_name = name;
  pinMode(E_STEP_PIN, OUTPUT);
  pinMode(E_DIR_PIN, OUTPUT);
  digitalWrite(E_STEP_PIN, 0);
  digitalWrite(E_DIR_PIN, 0);
  setTemperature(0);
  stp = false;
}

inline void extruder::buildCommand(char c)
{
  commandBuffer[0] = c;
  commandBuffer[1] = 0;  
}

inline void extruder::buildCommand(char c, char v)
{
  commandBuffer[0] = c;
  commandBuffer[1] = v;
  commandBuffer[2] = 0;  
}

inline void extruder::buildNumberCommand(char c, int v)
{
  commandBuffer[0] = c;
  itoa(v, &commandBuffer[1], 10);
}




inline  void extruder::valveSet(bool open, int dTime)
{
   if(open)
     buildCommand(VALVE, '1');
   else
     buildCommand(VALVE, '0');
   talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);
   
   delay(dTime);
}

inline  void extruder::setCooler(byte e_speed)
{   
   buildNumberCommand(COOL, e_speed);
   talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);
}

inline  void extruder::setTemperature(int temp)
{
   target_celsius = temp;
   buildNumberCommand(SET_T, temp);
   talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer); 
}

inline  int extruder::getTemperature()
{
   buildCommand(GET_T);
   char* reply = talker.sendPacketAndGetReply(my_name, commandBuffer);
   return(atoi(reply));
}

inline  void extruder::manage()
{

}

inline void extruder::setDirection(bool direction)
{
//   if(direction)
//     buildCommand(DIRECTION, '1');
//   else
//     buildCommand(DIRECTION, '0');
//   talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);
   if(direction)
     digitalWrite(E_DIR_PIN, 1);
   else
     digitalWrite(E_DIR_PIN, 0);
}


inline  void extruder::sStep()
{
   //buildCommand(STEP);
   //talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer); 
   stp = !stp;
   if(stp)
     digitalWrite(E_STEP_PIN, 1);
   else
     digitalWrite(E_STEP_PIN, 0);
}

inline  void extruder::enableStep()
{
  // Not needed - stepping the motor enables it automatically

}

inline  void extruder::disableStep()
{
  // N.B. Disabling the extrude stepper causes the backpressure to
  // turn the motor the wrong way.  Usually leave it on.
#if DISABLE_E  
  buildCommand(DISABLE);
  talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);
#endif
}

inline int extruder::potVoltage()
{
   buildCommand(PREAD);
   char* reply = talker.sendPacketAndGetReply(my_name, commandBuffer);
   return(atoi(reply));  
}

inline void extruder::setPWM(int p)
{
   buildNumberCommand(SPWM, p);
   talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);   
}

inline  void extruder::usePotForMotor()
{ 
  buildCommand(UPFM);
  talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);
}

inline bool extruder::ping()
{
  buildCommand(PING);
  return talker.sendPacketAndCheckAcknowledgement(my_name, commandBuffer);  
}

#endif

extern extruder* ex[ ];
extern byte extruder_in_use;

#endif
