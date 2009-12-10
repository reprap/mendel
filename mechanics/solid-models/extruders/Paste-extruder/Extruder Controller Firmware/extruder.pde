/*
 * Pasteextruder
 *
*/
 
#include "configuration.h"
#define SDAPin 18 
int Currentstate = 3;
int Requiredstate = 0;
int Motorstate = 0;
const int ledPin = 13;                // LED connected to digital pin 13
const int motorstate = 0;
const int analogthreshold = 512;
const int Selectionpin = 7;
const int Powerpin = 6;
const int Motorreverse = 20 ;

/* Definition of extruderstates
0 = Neutral / Exhaust
1 = Extruder 1 Extrude On
2 = Extruder 2 Extrude On
3 = State Unknown / Startup

Definition of Motorstates
-1 =  Anticlockwisse
0 = Off
1 = Clockwise

*/
void setup()                    // run once, when the sketch starts
{
  Serial.begin(9600);
  Serial.println("Starting");
  pinMode(ledPin, OUTPUT);      // sets the digital pin as output
  pinMode(H1D, OUTPUT);
  pinMode(H1E, OUTPUT);  
  pinMode(H2D, OUTPUT);
  pinMode(H2E, OUTPUT);
  pinMode(SDAPin, INPUT);  
}

int StopMotor(int Motorstate){
   int result;
     if (Motorstate == 1){
              digitalWrite(H1D, 0);    
              digitalWrite(H2D, 0);
              digitalWrite(H1E, HIGH);
              digitalWrite(H2E, LOW);
              delay(Motorreverse);
              digitalWrite(H1E, LOW);
              digitalWrite(H2E, LOW);
               result = 0;
              Serial.println("Motor Stopped from clockwise");
                          }
  if (Motorstate == -1){
              digitalWrite(H1D, 1);    
              digitalWrite(H2D, 1);
              digitalWrite(H1E, HIGH);
              digitalWrite(H2E, LOW);
              delay(Motorreverse);
              digitalWrite(H1E, LOW);
              digitalWrite(H2E, LOW);
              result = 0;
              Serial.println("Motor Stopped from anticlockwise");
  } 
   return result;       
 }
 
 int StartMotor(int RequiredMotorstate){
   int result;
   if (RequiredMotorstate == 1){
              digitalWrite(H1D, 1);    
              digitalWrite(H2D, 1);
              digitalWrite(H1E, HIGH);
              digitalWrite(H2E, LOW);
              result = 1;
              Serial.println("Motor started clockwise");
   }
   if (RequiredMotorstate == -1){
              digitalWrite(H1D, 0);    
              digitalWrite(H2D, 0);
              digitalWrite(H1E, HIGH);
              digitalWrite(H2E, LOW);
              result = -1;
              Serial.println("Motor started anticlockwise");
   }
   return result;
 }
 
 



void loop()                     // run over and over again
  {
    Serial.println("Current state - "); 
    Serial.println(Currentstate);
   if (analogRead(Powerpin)<analogthreshold){ //If both optoswitches closed, Required state is 1 i.e. Extruder 1 On
        Requiredstate=1;
        Serial.println("Required state - Extruder 1");
      }
      
      if (analogRead(Powerpin)>analogthreshold){ //If both Power is open, direction is closed, Required state is 2 i.e. Extruder 2 On
        Requiredstate=2;
        Serial.println("Required state - Extruder 2");
      }
    
    
    
    
    if(Currentstate == 3){  // At start up, check exhaust is open, if not power the motors. When motor is required to stop, motor is reversed.   
     Serial.println("State unknown");
     if (digitalRead(SDAPin)){
       digitalWrite(ledPin, LOW);   // sets the LED off
       digitalWrite(H1E, LOW);          //Kill Power
       digitalWrite(H2E, LOW);
           if (Motorstate == 1){
             Motorstate = StopMotor(Motorstate);
           }
              Currentstate = 0;
              Serial.println("Exhaust Open");
      
     }
     else{ //Turn motor clockwise until in neutral position
       Motorstate = StartMotor(1);
       
         }
   }
   
   if(Requiredstate == 0){
     if(Currentstate == 0){
       Serial.println("Exhaust Required - Already in Exhaust Position");
     }
     if(Currentstate == 1){
       Serial.println("Exhaust Required - Turn Anticlockwise");
       while (digitalRead(SDAPin) == 0) {
       Motorstate = StartMotor(-1);
     }
       Motorstate = StopMotor (Motorstate);
       Currentstate = 0;
   }
   
     if(Currentstate == 2){
       Serial.println("Exhaust Required - Turn clockwise");
       while (digitalRead(SDAPin) == 0) {
       Motorstate = StartMotor(1);
     }
       Motorstate = StopMotor (Motorstate);
       Currentstate = 0;
   }    
   }
   
   if(Requiredstate == 1){
     if(Currentstate == 1){
       Serial.println("Extruder 1 Required - Already Selected");
     }
     if(Currentstate != 1){
       Serial.println("Extruder 1 - Turn clockwise");
       while ((digitalRead(SDAPin) == 1)) {
       Motorstate = StartMotor(1);
     }
       Motorstate = StopMotor (Motorstate);
       Currentstate = 1;
   } 
   }
   
   if(Requiredstate == 2){
     if(Currentstate == 2){
       Serial.println("Extruder 2 Required - Already Selected");
     }
     if(Currentstate != 2){
       Serial.println("Extruder 2 - Turn anticlockwise");
       while ((digitalRead(SDAPin) == 1)) {
       Motorstate = StartMotor(-1);
     }
       Motorstate = StopMotor (Motorstate);
       Currentstate = 2;
     }
   
  }
  }
   
   
   
   /*
   if(Currentstate == 0){
   
      if(Requiredstate == 0){
       if(Motorstate != 0){            //If motor was turning apply break
         Motorstate = StopMotor(Motorstate);
       }
   }         
    if (Requiredstate == 1){
       Motorstate = StartMotor(1);
     }
     
    if (Requiredstate ==-1){
       Motorstate = StartMotor(-1);
     }
   }
   
    
      if(Requiredstate == 1){
        if(Currentstate == 1){
        Motorstate = StopMotor(Motorstate);
        }
        
           if(Motorstate ==1){            //If motor was turning apply break
               Motorstate = StopMotor(Motorstate);
               Currentstate = 1  
                }
           if(Motorstate ==-1){            //If motor was turning apply break
               Motorstate = StopMotor(Motorstate);
               Currentstate = 2 
      }
        else {
       Motorstate = StartMotor(-1);
        }
    }
    
     if(Currentstate == 2){
      if(Requiredstate == 2){
       if(Motorstate != 0){            //If motor was turning apply break
         Motorstate = StopMotor(Motorstate);
       }
      }
        else {
       Motorstate = StartMotor(1);
        }
     }
    }
     
 */
 
   
   

 

   
   
   
  



