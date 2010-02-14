
#include "configuration.h"
#include "extruder.h"
#include "temperature.h"

extruder::extruder()
{
  pinMode(H1D, OUTPUT);
  pinMode(H1E, OUTPUT);  
  pinMode(H2D, OUTPUT);
  pinMode(H2E, OUTPUT);
  pinMode(OUTPUT_A, OUTPUT);
  pinMode(OUTPUT_B, OUTPUT);
  pinMode(OUTPUT_C, OUTPUT);
  pinMode(E_STEP_PIN, INPUT);
  pinMode(E_DIR_PIN, INPUT);  
  pinMode(POT, INPUT);
#ifdef MAX6675_THERMOCOUPLE
  pinMode(SO, INPUT);
  pinMode(SCK, OUTPUT);
  pinMode(TC_0, OUTPUT); 
  digitalWrite(TC_0,HIGH);  // Disable MAX6675
#else
  pinMode(TEMP_PIN, INPUT);
#endif

  disableStep();

  // Change the frequency of Timer 0 so that PWM on pins H1E and H2E goes at
  // a very high frequency (64kHz see: 
  // http://tzechienchu.typepad.com/tc_chus_point/2009/05/changing-pwm-frequency-on-the-arduino-diecimila.html)

  TCCR0B &= ~(0x07); 
  TCCR0B |= 1;
 
#ifdef  PID_CONTROL

   pGain = TEMP_PID_PGAIN;
   iGain = TEMP_PID_IGAIN;
   dGain = TEMP_PID_DGAIN;
   temp_iState = 0;
   temp_dState = 0;
   temp_iState_min = -TEMP_PID_INTEGRAL_DRIVE_MAX/iGain;
   temp_iState_max = TEMP_PID_INTEGRAL_DRIVE_MAX/iGain;
   iState = 0;
   dState = 0;
   previousTime = millis()/MILLI_CORRECTION;

#endif
 

  // Defaults

  coilPosition = 0;  
  forward = true;
  pwmValue =  STEP_PWM;
  targetTemperature = 0;
  currentTemperature = 0;
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

#ifdef  PID_CONTROL

// With thanks to Adam at Makerbot and Tim at BotHacker
// see http://blog.makerbot.com/2009/10/01/open-source-ftw/

byte extruder::pidCalculation(int dt)
{
  int output;
  int error;
  float pTerm, iTerm, dTerm;

  error = targetTemperature - currentTemperature;

  pTerm = pGain * error;

  temp_iState += error;
  temp_iState = constrain(temp_iState, temp_iState_min, temp_iState_max);
  iTerm = iGain * temp_iState;

  dTerm = dGain * (currentTemperature - temp_dState);
  temp_dState = currentTemperature;

  output = pTerm + iTerm - dTerm;
  output = constrain(output, 0, 255);

  return output;
}

#endif

void extruder::controlTemperature()
{
  currentTemperature = internalTemperature(); 
    
#ifdef PID_CONTROL

  int dt;
  unsigned long time = millis()/MILLI_CORRECTION;  // Correct for fast clock
  dt = time - previousTime;
  previousTime = time;
  if (dt > 0) // Don't do it when millis() has rolled over
    analogWrite(OUTPUT_C, pidCalculation(dt));

#else

  // Simple bang-bang temperature control

  if(targetTemperature > currentTemperature)
    digitalWrite(OUTPUT_C, 1);
  else
    digitalWrite(OUTPUT_C, 0);

#endif 
}



void extruder::slowManage()
{
  manageCount = 0;
  
  potSum += (potVoltage() >> 2);
  potCount++;
  if(potCount >= 10)
  {
    potVal = (byte)(potSum/10);
    potCount = 0;
    potSum = 0;
  }

  //blink(true);  

  controlTemperature();
}

void extruder::manage()
{
  byte s = digitalRead(E_STEP_PIN);
  if(s != stp)
  {
    stp = s;
    sStep();
  }

#ifdef PASTE_EXTRUDER
  valveMonitor();
#endif

  manageCount++;
  if(manageCount > SLOW_CLOCK)
    slowManage();   
}


/* 
 Temperature reading function  
 With thanks to: Ryan Mclaughlin - http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1230859336
 for the MAX6675 code
 */

int extruder::internalTemperature()
{
#ifdef USE_THERMISTOR
  int raw = analogRead(TEMP_PIN);

  int celsius = raw;
  byte i;

  // TODO: This should do a binary chop

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
#endif

#ifdef AD595_THERMOCOUPLE
  return ( 5.0 * analogRead(TEMP_PIN) * 100.0) / 1024.0; //(int)(((long)500*(long)analogRead(TEMP_PIN))/(long)1024);
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
    return 2000;
  else
    return value/4;

#endif

}

void extruder::waitForTemperature()
{

}

void extruder::valveSet(bool closed)
{
#ifdef PASTE_EXTRUDER
  requiredValveState = closed;
  kickStartValve();
#endif
}

void extruder::setDirection(bool direction)
{
  forward = direction;  
}

void extruder::setCooler(byte e_speed)
{
  analogWrite(OUTPUT_B, e_speed);   
}

void extruder::setTemperature(int tp)
{
  targetTemperature = tp;
}

int extruder::getTemperature()
{
  return currentTemperature;  
}

void extruder::sStep()
{
#ifndef PASTE_EXTRUDER
  byte pwm;
  
  if(usePot)
    pwm = potVal;
  else
    pwm = pwmValue;

  // This increments or decrements coilPosition then writes the appropriate pattern to the output pins.

  if(digitalRead(E_DIR_PIN))
    coilPosition++;
  else
    coilPosition--;
  coilPosition &= 7;

  // Which of the 8 possible patterns do we want?
  // The pwm = (pwm >> 1) + (pwm >> 3); lines
  // ensure (roughly) equal power on the half-steps

#ifdef FULL_STEP
  switch((coilPosition&3) << 1)
#else
  switch(coilPosition)
#endif 
  {
  case 7:
    pwm = (pwm >> 1) + (pwm >> 3);
    digitalWrite(H1D, 1);    
    digitalWrite(H2D, 1);
    analogWrite(H1E, pwm);
    analogWrite(H2E, pwm);    
    break;

  case 6:
    digitalWrite(H1D, 1);    
    digitalWrite(H2D, 1);
    analogWrite(H1E, pwm);
    analogWrite(H2E, 0);   
    break; 

  case 5:
    pwm = (pwm >> 1) + (pwm >> 3);
    digitalWrite(H1D, 1);
    digitalWrite(H2D, 0);
    analogWrite(H1E, pwm);
    analogWrite(H2E, pwm); 
    break;

  case 4:
    digitalWrite(H1D, 1);
    digitalWrite(H2D, 0);
    analogWrite(H1E, 0);
    analogWrite(H2E, pwm); 
    break;

  case 3:
    pwm = (pwm >> 1) + (pwm >> 3);
    digitalWrite(H1D, 0);
    digitalWrite(H2D, 0);
    analogWrite(H1E, pwm);
    analogWrite(H2E, pwm); 
    break; 

  case 2:
    digitalWrite(H1D, 0);
    digitalWrite(H2D, 0);
    analogWrite(H1E, pwm);
    analogWrite(H2E, 0); 
    break;

  case 1:
    pwm = (pwm >> 1) + (pwm >> 3);
    digitalWrite(H1D, 0);
    digitalWrite(H2D, 1);
    analogWrite(H1E, pwm);
    analogWrite(H2E, pwm); 
    break;

  case 0:
    digitalWrite(H1D, 0);
    digitalWrite(H2D, 1);
    analogWrite(H1E, 0);
    analogWrite(H2E, pwm); 
    break; 

  }
#endif
}


void extruder::enableStep()
{
  // Nothing to do here - step() automatically enables the stepper drivers appropriately.  
}

void extruder::disableStep()
{
  analogWrite(H1E, 0);
  analogWrite(H2E, 0);  
}

int extruder::potVoltage()
{
  return (int)analogRead(POT);  
}

void extruder::setPWM(int p)
{
  pwmValue = p;
  usePot = false;
}

void extruder::usePotForMotor()
{
  usePot = true;
}

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

  case STEP:
    //sStep(); // Now handled by hardware.
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

  case PING:
    break;

  default:
    return 0; // Flag up dud command
  }
  return reply; 
}

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

// MILLI_CORRECTION not needed here; makes time resolution too coarse
  valveEndTime = millis() + millisecs; //*MILLI_CORRECTION;
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
   
   


