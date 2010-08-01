#ifndef HOSTCOM_H
#define HOSTCOM_H
/*
 * Class to handle sending messages from and back to the host.
 * NOWHERE ELSE in this program should anything send to Serial.print()
 * or get anything from Serial.read().
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
  void sendMessage(bool doMessage);
  void start();
  
// Wrappers for the comms interface

  void putInit();
  void put(char* s);
  void put(const float& f);
  void put(const long& l);
  void put(int i);
  void putEnd();
  byte gotData();
  char get();
  
private:
  void reset();
  void sendtext(bool noText);
  char message[RESPONSE_SIZE];
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

// Wrappers for the comms interface

inline void hostcom::putInit() {  Serial.begin(HOST_BAUD); }
inline void hostcom::put(char* s) { Serial.print(s); }
inline void hostcom::put(const float& f) { Serial.print(f); }
inline void hostcom::put(const long& l) { Serial.print(l); }
inline void hostcom::put(int i) { Serial.print(i); }
inline void hostcom::putEnd() { Serial.println(); }
inline byte hostcom::gotData() { return Serial.available(); }
inline char hostcom::get() { return Serial.read(); }

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
  putInit();
  put("start");
  putEnd();  
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

inline void hostcom::sendtext(bool doMessage)
{
  if(!doMessage)
    return;
  if(!message[0])
    return;
  put(" ");
  put(message);
}

inline void hostcom::sendMessage(bool doMessage)
{
  if(fatal)
  {
    put("!!");
    sendtext(true);
    putEnd();
    shutdown();
    return; // Technically redundant - shutdown never returns.
  }
  
  if(resend < 0)
    put("ok");
  else
  {
    put("rs ");
    put(resend);
  }
    
  if(etemp > NO_TEMP)
  {
    put(" T:");
    put(etemp);
  }
  
  if(btemp > NO_TEMP)
  {
    put(" B:");
    put(btemp);
  }
  
  if(sendCoordinates)
  {				
    put(" C: X:");
    put(x);
    put(" Y:");
    put(y);
    put(" Z:");
    put(z);
    put(" E:");
    put(e);
  }
  
  sendtext(doMessage);
  
  putEnd();
  
  reset(); 
}


#endif
