

#include "configuration.h"
#include "pins.h"
#include "Temperature.h"
#include "intercom.h"
#include "extruder.h" 

// Keep all extruders up to temperature etc.


void manageAllExtruders()
{
  for(byte i = 0; i < EXTRUDER_COUNT; i++)
    ex[i]->manage();
}

// Select a new extruder

void newExtruder(byte e)
{
  if(e < 0)
    e = 0;
  if(e >= EXTRUDER_COUNT)
    e = EXTRUDER_COUNT - 1;

  if(e != extruder_in_use)
  {  
    extruder_in_use = e;
    setUnits(cdda[0]->get_units());
  }
}

//*************************************************************************

// Extruder functions that are the same for all extruders.

void extruder::waitForTemperature()
{
  byte seconds = 0;
  bool warming = true;
  count = 0;
  newT = 0;
  oldT = newT;

  while (true)
  {
    manageAllExtruders();
    newT += getTemperature();
    count++;
    if(count > 5)
    {
      newT = newT/5;
      if(newT >= targetTemperature - HALF_DEAD_ZONE)
      {
        warming = false;
        if(seconds > WAIT_AT_TEMPERATURE)
          return;
        else 
          seconds++;
      } 

      if(warming)
      {
        if(newT > oldT)
          oldT = newT;
        else
        {
          // Temp isn't increasing - extruder hardware error
          temperatureError();
          return;
        }
      }

      newT = 0;
      count = 0;
    }
    delay(1000);
  }
}

// TODO: Should use debugstring[]

void extruder::temperatureError()
{
  Serial.print("E: ");
  Serial.println(getTemperature());  
}

/***************************************************************************************************************************
 * 
 * Darwin-style motherboard
 */

#if MOTHERBOARD == 1 

extruder::extruder(byte md_pin, byte ms_pin, byte h_pin, byte f_pin, byte t_pin, byte vd_pin, byte ve_pin, byte se_pin, float spm)
{
  motor_dir_pin = md_pin;
  motor_speed_pin = ms_pin;
  heater_pin = h_pin;
  fan_pin = f_pin;
  temp_pin = t_pin;
  valve_dir_pin = vd_pin;
  valve_en_pin = ve_pin;
  step_en_pin = se_pin;
  sPerMM = spm;
  
  //setup our pins
  pinMode(motor_dir_pin, OUTPUT);
  pinMode(motor_speed_pin, OUTPUT);
  pinMode(heater_pin, OUTPUT);

  pinMode(temp_pin, INPUT);
  pinMode(valve_dir_pin, OUTPUT); 
  pinMode(valve_en_pin, OUTPUT);

  //initialize values
  digitalWrite(motor_dir_pin, EXTRUDER_FORWARD);

  analogWrite(heater_pin, 0);
  analogWrite(motor_speed_pin, 0);
  digitalWrite(valve_dir_pin, false);
  digitalWrite(valve_en_pin, 0);

  // The step enable pin and the fan pin are the same...
  // We can have one, or the other, but not both

  if(step_en_pin >= 0)
  {
    pinMode(step_en_pin, OUTPUT);
    disableStep();
  } 
  else
  {
    pinMode(fan_pin, OUTPUT);
    analogWrite(fan_pin, 0);
  }

  //these our the default values for the extruder.
  e_speed = 0;
  target_celsius = 0;
  max_celsius = 0;
  heater_low = 64;
  heater_high = 255;
  heater_current = 0;
  valve_open = false;

  //this is for doing encoder based extruder control
  //        rpm = 0;
  //        e_delay = 0;
  //        error = 0;
  //        last_extruder_error = 0;
  //        error_delta = 0;
  e_direction = EXTRUDER_FORWARD;

  //default to cool
  setTemperature(target_celsius);
}

void extruder::shutdown()
{
  analogWrite(heater_pin, 0); 
  digitalWrite(step_en_pin, !ENABLE_ON);
  valveSet(false, 500);
}


/*
byte extruder::wait_till_cool()
 {  
 count = 0;
 oldT = get_temperature();
 while (get_temperature() > target_celsius + HALF_DEAD_ZONE)
 {
 	manage_all_extruders();
 count++;
 if(count > 20)
 {
 newT = get_temperature();
 if(newT < oldT)
 oldT = newT;
 else
 return 1;
 count = 0;
 }
 	delay(1000);
 }
 return 0;
 }
 */



void extruder::valveSet(bool open, int dTime)
{
  waitForTemperature();
  valve_open = open;
  digitalWrite(valve_dir_pin, open);
  digitalWrite(valve_en_pin, 1);
  delay(dTime);
  digitalWrite(valve_en_pin, 0);
}


void extruder::setTemperature(int temp)
{
  target_celsius = temp;
  max_celsius = (temp*11)/10;

  // If we've turned the heat off, we might as well disable the extrude stepper
  // if(target_celsius < 1)
  //  disableStep(); 
}

/**
 *  Samples the temperature and converts it to degrees celsius.
 *  Returns degrees celsius.
 */
int extruder::getTemperature()
{
#ifdef USE_THERMISTOR
  int raw = sampleTemperature();

  int celsius = 0;
  byte i;

  for (i=1; i<NUMTEMPS; i++)
  {
    if (temptable[i][0] > raw)
    {
      celsius  = temptable[i-1][1] + 
        (raw - temptable[i-1][0]) * 
        (temptable[i][1] - temptable[i-1][1]) /
        (temptable[i][0] - temptable[i-1][0]);

      break;
    }
  }

  // Overflow: Set to last value in the table
  if (i == NUMTEMPS) celsius = temptable[i-1][1];
  // Clamp to byte
  if (celsius > 255) celsius = 255; 
  else if (celsius < 0) celsius = 0; 

  return celsius;
#else
  return ( 5.0 * sampleTemperature() * 100.0) / 1024.0;
#endif
}



/*
* This function gives us an averaged sample of the analog temperature pin.
 */
int extruder::sampleTemperature()
{
  int raw = 0;

  //read in a certain number of samples
  for (byte i=0; i<TEMPERATURE_SAMPLES; i++)
    raw += analogRead(temp_pin);

  //average the samples
  raw = raw/TEMPERATURE_SAMPLES;

  //send it back.
  return raw;
}

/*!
 Manages extruder functions to keep temps, speeds etc
 at the set levels.  Should be called only by manage_all_extruders(),
 which should be called in all non-trivial loops.
 o If temp is too low, don't start the motor
 o Adjust the heater power to keep the temperature at the target
 */
void extruder::manage()
{
  //make sure we know what our temp is.
  int current_celsius = getTemperature();
  byte newheat = 0;

  //put the heater into high mode if we're not at our target.
  if (current_celsius < target_celsius)
    newheat = heater_high;
  //put the heater on low if we're at our target.
  else if (current_celsius < max_celsius)
    newheat = heater_low;

  // Only update heat if it changed
  if (heater_current != newheat) {
    heater_current = newheat;
    analogWrite(heater_pin, heater_current);
  }
}

#endif


/***************************************************************************************************************************
 * 
 * Arduino Mega motherboard
 */
#if MOTHERBOARD == 3

static PIDcontrol ePID(EXTRUDER_0_HEATER_PIN, EXTRUDER_0_TEMPERATURE_PIN, false);

// With thanks to Adam at Makerbot and Tim at BotHacker
// see http://blog.makerbot.com/2009/10/01/open-source-ftw/

PIDcontrol::PIDcontrol(byte hp, byte tp, bool b)
{
   heat_pin = hp;
   temp_pin = tp;
   pGain = TEMP_PID_PGAIN;
   iGain = TEMP_PID_IGAIN;
   dGain = TEMP_PID_DGAIN;
   temp_iState = 0;
   temp_dState = 0;
   temp_iState_min = -TEMP_PID_INTEGRAL_DRIVE_MAX/iGain;
   temp_iState_max = TEMP_PID_INTEGRAL_DRIVE_MAX/iGain;
   iState = 0;
   dState = 0;
   previousTime = millis();
   output = 0;
   currentTemperature = 0;
   bedTable = b;
   pinMode(heat_pin, OUTPUT);
   pinMode(temp_pin, INPUT); 
}

/* 
 Temperature reading function  
 With thanks to: Ryan Mclaughlin - http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1230859336
 for the MAX6675 code
 */

void PIDcontrol::internalTemperature(short table[][2])
{
#ifdef USE_THERMISTOR
  int raw = analogRead(temp_pin);

  byte i;

  // TODO: This should do a binary chop

  for (i=1; i<NUMTEMPS; i++)
  {
    if (table[i][0] > raw)
    {
      currentTemperature  = table[i-1][1] + 
        (raw - table[i-1][0]) * 
        (table[i][1] - table[i-1][1]) /
        (table[i][0] - table[i-1][0]);

      break;
    }
  }

  // Overflow: Set to last value in the table
  if (i >= NUMTEMPS) currentTemperature = table[i-1][1];
  // Clamp to byte
  //if (celsius > 255) celsius = 255; 
  //else if (celsius < 0) celsius = 0; 

#endif

#ifdef AD595_THERMOCOUPLE
  currentTemperature = ( 5.0 * analogRead(pin* 100.0) / 1024.0; //(int)(((long)500*(long)analogRead(TEMP_PIN))/(long)1024);
#endif  

#ifdef MAX6675_THERMOCOUPLE
  int value = 0;
  byte error_tc;


  digitalWrite(TC_0, 0); // Enable device

  /* Cycle the clock for dummy bit 15 */
  digitalWrite(SCK,1);
  digitalWrite(SCK,0);

  /* Read bits 14-3 from MAX6675 for the Temp
   	 Loop for each bit reading the value 
   */
  for (int i=11; i>=0; i--)
  {
    digitalWrite(SCK,1);  // Set Clock to HIGH
    value += digitalRead(SO) << i;  // Read data and add it to our variable
    digitalWrite(SCK,0);  // Set Clock to LOW
  }

  /* Read the TC Input inp to check for TC Errors */
  digitalWrite(SCK,1); // Set Clock to HIGH
  error_tc = digitalRead(SO); // Read data
  digitalWrite(SCK,0);  // Set Clock to LOW

  digitalWrite(TC_0, 1); //Disable Device

  if(error_tc)
    currentTemperature = 2000;
  else
    currentTemperature = value/4;

#endif

}


void PIDcontrol::pidCalculation(int target)
{
  if(bedTable)
    internalTemperature(bedtemptable);
  else
    internalTemperature(temptable);
  time = millis();
  dt = time - previousTime;
  previousTime = time;
  if (dt <= 0) // Don't do it when millis() has rolled over
    return;
    
  error = target - currentTemperature;

  pTerm = pGain * error;

  temp_iState += error;
  temp_iState = constrain(temp_iState, temp_iState_min, temp_iState_max);
  iTerm = iGain * temp_iState;

  dTerm = dGain * (currentTemperature - temp_dState);
  temp_dState = currentTemperature;

  output = pTerm + iTerm - dTerm;
  output = constrain(output, 0, 255);
  
  analogWrite(heat_pin, output);
}

//*******************************************************************************************


#if 0
void extruder::shutdown()
{
  analogWrite(heater_pin, 0); 
  disableStep();
  valveSet(false, 500);
}


void extruder::valveSet(bool open, int dTime)
{
#ifdef PASTE_EXTRUDER
  waitForTemperature();
  valve_open = open;
  digitalWrite(valve_dir_pin, open);
  digitalWrite(valve_en_pin, 1);
  delay(dTime);
  digitalWrite(valve_en_pin, 0);
#endif
}


void extruder::setTemperature(int temp)
{
  target_celsius = temp;
  //max_celsius = (temp*11)/10;
}

/**
 *  Samples the temperature and converts it to degrees celsius.
 *  Returns degrees celsius.
 */
int extruder::getTemperature()
{
#ifdef USE_THERMISTOR
  int raw = sampleTemperature();

  int celsius = 0;
  byte i;

  for (i=1; i<NUMTEMPS; i++)
  {
    if (temptable[i][0] > raw)
    {
      celsius  = temptable[i-1][1] + 
        (raw - temptable[i-1][0]) * 
        (temptable[i][1] - temptable[i-1][1]) /
        (temptable[i][0] - temptable[i-1][0]);

      break;
    }
  }

  // Overflow: Set to last value in the table
  if (i == NUMTEMPS) celsius = temptable[i-1][1];
  // Clamp to byte
  if (celsius > 255) celsius = 255; 
  else if (celsius < 0) celsius = 0; 

  return celsius;
#else
  return ( 5.0 * sampleTemperature() * 100.0) / 1024.0;
#endif
}



/*
* This function gives us an averaged sample of the analog temperature pin.
 */
/*
int extruder::sampleTemperature()
{
  int raw = 0;

  //read in a certain number of samples
  for (byte i=0; i<TEMPERATURE_SAMPLES; i++)
    raw += analogRead(temp_pin);

  //average the samples
  raw = raw/TEMPERATURE_SAMPLES;

  //send it back.
  return raw;
}
*/

/*!
 Manages extruder functions to keep temps, speeds etc
 at the set levels.  Should be called only by manage_all_extruders(),
 which should be called in all non-trivial loops.
 o If temp is too low, don't start the motor
 o Adjust the heater power to keep the temperature at the target
 */
void extruder::manage()
{
  //make sure we know what our temp is.
  int current_celsius = getTemperature();
  byte newheat = 0;

  //put the heater into high mode if we're not at our target.
  if (current_celsius < target_celsius)
    newheat = heater_high;
  //put the heater on low if we're at our target.
  else if (current_celsius < max_celsius)
    newheat = heater_low;

  // Only update heat if it changed
  if (heater_current != newheat) {
    heater_current = newheat;
    analogWrite(heater_pin, heater_current);
  }
}


//**********************************************************************************************

extruder::extruder()
{
  pinMode(H1D, OUTPUT);
  pinMode(H1E, OUTPUT);  
  pinMode(H2D, OUTPUT);
  pinMode(H2E, OUTPUT);
  pinMode(FAN_OUTPUT, OUTPUT);
  pinMode(E_STEP_PIN, INPUT);
  pinMode(E_DIR_PIN, INPUT);  
  pinMode(POT, INPUT);
  
#ifdef MAX6675_THERMOCOUPLE
  pinMode(SO, INPUT);
  pinMode(SCK, OUTPUT);
  pinMode(TC_0, OUTPUT); 
  digitalWrite(TC_0,HIGH);  // Disable MAX6675
#endif
  
  disableStep();
 
  extruderPID = &ePID;
  bedPID = &bPID;

  // Defaults

  coilPosition = 0;  
  forward = true;
  pwmValue =  STEP_PWM;
  targetTemperature = 0;
  targetBedTemperature = 0;
  manageCount = 0;
  stp = 0;
  potVal = 0;
  potSum = 0;
  potCount = 0;
  usePot = true;
  
#ifdef PASTE_EXTRUDER
  pinMode(OPTO_PIN, INPUT); 
  valveAlreadyRunning = false;
  valveEndTime = 0;
  valveAtEnd = false;
  seenHighLow = false;
  valveState = false;
  requiredValveState = true;
  kickStartValve();
#endif
}
#endif

//*******************************************************************************************

extruder::extruder(byte stp, byte dir, byte en, byte heat, byte temp, float spm)
{
  motor_step_pin = stp;
  motor_dir_pin = dir;
  motor_en_pin = en;
  heater_pin = heat;
  temp_pin = temp;
  sPerMM = spm;
  manageCount = 0;
  extruderPID = &ePID;
  
  //fan_pin = ;

  //setup our pins
  pinMode(motor_step_pin, OUTPUT);
  pinMode(motor_dir_pin, OUTPUT);
  pinMode(motor_en_pin, OUTPUT);
  pinMode(heater_pin, OUTPUT);
  pinMode(temp_pin, INPUT);
  
  disableStep();

  //initialize values
  digitalWrite(motor_dir_pin, 1);
  digitalWrite(motor_step_pin, 0);
  
  analogWrite(heater_pin, 0);



  //these our the default values for the extruder.

  targetTemperature = 0;
//  max_celsius = 0;
//  heater_low = 64;
//  heater_high = 255;
//  heater_current = 0;


  //this is for doing encoder based extruder control
  //        rpm = 0;
  //        e_delay = 0;
  //        error = 0;
  //        last_extruder_error = 0;
  //        error_delta = 0;
//  e_direction = EXTRUDER_FORWARD;

  //default to cool
  setTemperature(targetTemperature);
  
#ifdef PASTE_EXTRUDER
  valve_dir_pin = vd_pin;
  valve_en_pin = ve_pin;
  pinMode(valve_dir_pin, OUTPUT); 
  pinMode(valve_en_pin, OUTPUT);
  digitalWrite(valve_dir_pin, false);
  digitalWrite(valve_en_pin, 0);
  valve_open = false;
#endif
}

void extruder::controlTemperature()
{   
  extruderPID->pidCalculation(targetTemperature);
  //bedPID->pidCalculation(targetBedTemperature);
}



void extruder::slowManage()
{
  manageCount = 0;
  
/*  
  potSum += (potVoltage() >> 2);
  potCount++;
  if(potCount >= 10)
  {
    potVal = (byte)(potSum/10);
    potCount = 0;
    potSum = 0;
  }
*/

  //blink(true);  

  controlTemperature();
}

void extruder::manage()
{
  /*
  byte s = digitalRead(E_STEP_PIN);
  if(s != stp)
  {
    stp = s;
    sStep(0);
  }
  */
  
#ifdef PASTE_EXTRUDER
  valveMonitor();
#endif

  manageCount++;
  if(manageCount > SLOW_CLOCK)
    slowManage();   
}



// Stop everything

void extruder::shutdown()
{
  // Heater off;
  setTemperature(0);
  //setBedTemperature(0);
  // Motor off
  disableStep();
  // Close valve
  valveSet(false, 500);
}


void extruder::valveSet(bool closed, int dTime)
{
#ifdef PASTE_EXTRUDER
  requiredValveState = closed;
  kickStartValve();
#endif
}

void extruder::setDirection(bool direction)
{
  digitalWrite(motor_dir_pin, direction);  
}

void extruder::setCooler(byte e_speed)
{
  //analogWrite(FAN_OUTPUT, e_speed);   
}

void extruder::setTemperature(int tp)
{
  targetTemperature = tp;
}

int extruder::getTemperature()
{
  return extruderPID->temperature();  
}

/*
void extruder::setBedTemperature(int tp)
{
  targetBedTemperature = tp;
}

int extruder::getBedTemperature()
{
  return bedPID->temperature();  
}

*/

void extruder::sStep()
{
	digitalWrite(motor_step_pin, HIGH);
	delayMicrosecondsInterruptible(5);
	digitalWrite(motor_step_pin, LOW);  
}

void extruder::enableStep()
{
    digitalWrite(motor_en_pin, 0);
}

void extruder::disableStep()
{
  digitalWrite(motor_en_pin, 1); 
}

/*
int extruder::potVoltage()
{
  return (int)analogRead(POT);  
}*/

/*
void extruder::setPWM(int p)
{
  pwmValue = p;
  usePot = false;
  sStep(1);
  sStep(2);
}*/

/*
void extruder::usePotForMotor()
{
  usePot = true;
  sStep(1);
  sStep(2);
}*/

/*
char* extruder::processCommand(char command[])
{
  reply[0] = 0;
  switch(command[0])
  {
  case WAIT_T:
    waitForTemperature();
    break;

  case VALVE:
    valveSet(command[1] != '1');
    break;

  case DIRECTION:
    // setDirection(command[1] == '1'); // Now handled by hardware.
    break;

  case COOL:
    setCooler(atoi(&command[1]));
    break;

  case SET_T:
    setTemperature(atoi(&command[1]));
    break;

  case GET_T:
    itoa(getTemperature(), reply, 10);
    break;
    
  case SET_BED_T:
    setBedTemperature(atoi(&command[1]));
    break;

  case GET_BED_T:
    itoa(getBedTemperature(), reply, 10);
    break;

  case STEP:
    //sStep(0); // Now handled by hardware.
    break;

  case ENABLE:
    enableStep();
    break;

  case DISABLE:
    disableStep();
    break;

  case PREAD:
    itoa(potVoltage(), reply, 10);
    break;

  case SPWM:
    setPWM(atoi(&command[1]));
    break;

  case UPFM:
    usePotForMotor();
    break;
  
  case SHUT:
    shutdown();
    break;  

  case PING:
    break;

  default:
    return 0; // Flag up dud command
  }
  return reply; 
}
*/

#ifdef PASTE_EXTRUDER

bool extruder::valveTimeCheck(int millisecs)
{
  if(valveAlreadyRunning)
  {
    if(millis() >= valveEndTime)
    {
      valveAlreadyRunning = false;
      return true;
    }
    return false;
  }

  valveEndTime = millis() + millisecs*MILLI_CORRECTION;
  valveAlreadyRunning = true;
  return false;
}

void extruder::valveTurn(bool close)
{
  if(valveAtEnd)
    return;
    
  byte valveRunningState = VALVE_STARTING;
  if(digitalRead(OPTO_PIN))
  {
    seenHighLow = true;
    valveRunningState = VALVE_RUNNING;
  } else
  {
    if(!seenHighLow)
     valveRunningState = VALVE_STARTING;
    else
     valveRunningState = VALVE_STOPPING; 
  }    
   
  switch(valveRunningState)
  {
  case VALVE_STARTING: 
          if(close)
             digitalWrite(H1D, 1);
          else
             digitalWrite(H1D, 0);
          digitalWrite(H1E, HIGH);
          break;
          
  case VALVE_RUNNING:
          return;
  
  case VALVE_STOPPING:
          if(close)
            digitalWrite(H1D, 0);
          else
            digitalWrite(H1D, 1);
            
          if(!valveTimeCheck(10))
            return;
            
          digitalWrite(H1E, LOW);
          valveState = close;
          valveAtEnd = true;
          seenHighLow = false;
          break;
          
  default:
          break;
  }  
}

void extruder::valveMonitor()
{
  if(valveState == requiredValveState)
    return;
  valveAtEnd = false;
  valveTurn(requiredValveState);
} 

void extruder::kickStartValve()
{
  if(digitalRead(OPTO_PIN))
  {
     if(requiredValveState)
       digitalWrite(H1D, 1);
     else
       digitalWrite(H1D, 0);
     digitalWrite(H1E, HIGH);    
  }
} 

#endif


#endif

