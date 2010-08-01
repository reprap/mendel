#ifndef HOSTCOM_H
#define HOSTCOM_H
/*
 * Class to handle sending messages back to the host
 * NOWHERE ELSE in this program should anything send to Serial.print
 */

// Can't get lower than absolute zero...

#define NO_TEMP -300

extern void shutdown();

class hostcom
{
public:
  hostcom();
  char* string();
  void setETemp(int et);
  void setBTemp(int bt);
  void setCoords(const FloatPoint& where);
  void setResend(long ln);
  void setFatal();
  void sendMessage(bool noText);
  void start();
  
private:
  void reset();
  void sendtext(bool noText);
  char message[COMMAND_SIZE];
  int etemp;
  int btemp;
  float x;
  float y;
  float z;
  float e;
  long resend;
  bool fatal;
  bool sendCoordinates;  
};

inline hostcom::hostcom()
{
  fatal = false;
  reset();
}

inline void hostcom::reset()
{
  etemp = NO_TEMP;
  btemp = NO_TEMP;
  message[0] = 0;
  resend = -1;
  sendCoordinates = false;
  // Don't reset fatal.
}

inline void hostcom::start()
{
  Serial.begin(HOST_BAUD);
  Serial.println("start");  
}

inline char* hostcom::string()
{
  return message;
}

inline void hostcom::setETemp(int et)
{
  etemp = et;
}

inline void hostcom::setBTemp(int bt)
{
  btemp = bt;
}

inline void hostcom::setCoords(const FloatPoint& where)
{
  x = where.x;
  y = where.y;
  z = where.z;
  e = where.e;
  sendCoordinates = true;
}

inline void hostcom::setResend(long ln)
{
  resend = ln;
}

inline void hostcom::setFatal()
{
  fatal = true;
}

inline void hostcom::sendtext(bool noText)
{
  if(noText)
    return;
  if(!message[0])
    return;
  Serial.print(" ");
  Serial.print(message);
}

inline void hostcom::sendMessage(bool noText)
{
  if(fatal)
  {
    Serial.print("!!");
    sendtext(false);
    Serial.println();
    shutdown();
    return; // Technically redundant - shutdown never returns.
  }
  
  if(resend < 0)
    Serial.print("ok");
  else
  {
    Serial.print("rs ");
    Serial.print(resend);
  }
    
  if(etemp > NO_TEMP)
  {
    Serial.print(" T:");
    Serial.print(etemp);
  }
  
  if(btemp > NO_TEMP)
  {
    Serial.print(" B:");
    Serial.print(btemp);
  }
  
  if(sendCoordinates)
  {				
    Serial.print(" C: X:");
    Serial.print(x);
    Serial.print(" Y:");
    Serial.print(y);
    Serial.print(" Z:");
    Serial.print(z);
    Serial.print(" E:");
    Serial.print(e);
  }
  
  sendtext(noText);
  
  Serial.println();
  
  reset(); 
}

//extern hostcom talkToHost;

#endif
