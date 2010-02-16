

#include "configuration.h"
#include "pins.h"
#include "ThermistorTable.h"
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
    //setExtruder();
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
      if(newT >= target_celsius - HALF_DEAD_ZONE)
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
 * If we have a new motherboard (V 1.x, x >= 1), the extruder is entirely controlled via the RS485, and all  the functions to do
 * it are simple inlines in extruder.h
 * 
 * Otherwise, we have to do the work ourselves...
 */

#if MOTHERBOARD < 2   
extruder::extruder(byte md_pin, byte ms_pin, byte h_pin, byte f_pin, byte t_pin, byte vd_pin, byte ve_pin, byte se_pin)
{
  motor_dir_pin = md_pin;
  motor_speed_pin = ms_pin;
  heater_pin = h_pin;
  fan_pin = f_pin;
  temp_pin = t_pin;
  valve_dir_pin = vd_pin;
  valve_en_pin = ve_pin;
  step_en_pin = se_pin;

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


#if 0
void extruder::setSpeed(float sp)
{
  // DC motor?
  if(step_en_pin < 0)
  {
    e_speed = (byte)sp;
    if(e_speed > 0)
      waitForTemperature();
    analogWrite(motor_speed_pin, e_speed);
    return;
  }

  // No - stepper
  disableTimerInterrupt();

  if(sp <= 1.0e-4)
  {
    disableStep();
    e_speed = 0; // Just use this as a flag
    return;
  } 
  else
  {
    waitForTemperature();
    enableStep();
    e_speed = 1;
  }

  extrude_step_count = 0;

  float milliseconds_per_step = 60000.0/(E_STEPS_PER_MM*sp);
  long thousand_ticks_per_step = 4*(long)(milliseconds_per_step);
  setupTimerInterrupt();
  setTimer(thousand_ticks_per_step);
  enableTimerInterrupt();
}

void extruder::interrupt()
{
  if(!e_speed)
    return;
  extrude_step_count++;
  if(extrude_step_count > 1000)
  {
    step();
    extrude_step_count = 0;
  }
}

#endif
#endif

