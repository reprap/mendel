


const int drivePin =  2;
const int switchPin =  3;
const int LEDPin =  13;

int MSState = LOW;
long previousM = 0;
long mark = 800;
long space = 1200;

void setup() {
  // set the digital pin as output:
  pinMode(drivePin, OUTPUT);
  pinMode(switchPin, INPUT);
  digitalWrite(switchPin, HIGH);
  pinMode(LEDPin, OUTPUT);
  previousM = micros();
  digitalWrite(drivePin, MSState);  
}

void loop()
{

  unsigned long currentM = micros();
  
  if(!digitalRead(switchPin))
  {
    digitalWrite(LEDPin, HIGH); 
    if (MSState == LOW)
    {
      if(currentM - previousM > space)
      {
        previousM = currentM;
        MSState = HIGH;
        digitalWrite(drivePin, MSState);
      }
    } else
    {
      if(currentM - previousM > mark)
      {
        previousM = currentM;
        MSState = LOW;
        digitalWrite(drivePin, MSState);
      }    
    }
  } else
  {
     digitalWrite(LEDPin, LOW); 
     digitalWrite(drivePin, LOW);
  }
    
}

