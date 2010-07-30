#ifndef BED_H
#define BED_H

#if MOTHERBOARD != 2  

class bed
{
  
public:
   bed(byte heat, byte temp);
   void waitForTemperature();
   
   void setTemperature(int temp);
   int getTemperature();
   void slowManage();
   void manage();
   void shutdown();
 
private:

   int targetTemperature;
   int count;
   int oldT, newT;
   long manageCount;
   
   PIDcontrol* bedPID;    // Temperature control - extruder...

   int sampleTemperature();
   void controlTemperature();
   void temperatureError(); 

// The pins we control
   byte heater_pin,  temp_pin;
 
};

#endif
#endif
