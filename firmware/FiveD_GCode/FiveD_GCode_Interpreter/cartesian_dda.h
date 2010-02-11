/*
 * This class controls the movement of the RepRap machine.
 * It implements a DDA in four dimensions, so the length of extruded 
 * filament is treated as a variable, just like X, Y, and Z.
 *
 * Adrian Bowyer 9 May 2009
 */

#ifndef CARTESIAN_DDA_H
#define CARTESIAN_DDA_H

// Main class for moving the RepRap machine about

class cartesian_dda
{
private:

  //extruder* ext;               // The extruder I'm currently using - keep this up to date...

  FloatPoint units;            // Factors for converting either mm or inches to steps

  FloatPoint target_position;  // Where it's going
  FloatPoint delta_position;   // The difference between the two
  float distance;              // How long the path is
  
  LongPoint current_steps;     // Similar information as above in steps rather than units
  LongPoint target_steps;
  LongPoint delta_steps;
  LongPoint dda_counter;       // DDA error-accumulation variables
  long t_scale;                // When doing lots of t steps, scale them so the DDA doesn't spend for ever on them
  
  volatile bool x_direction;            // Am I going in the + or - direction?
  volatile bool y_direction;
  volatile bool z_direction;
  volatile bool e_direction;
  volatile bool f_direction;

  volatile bool x_can_step;             // Am I not at an endstop?  Have I not reached the target? etc.
  volatile bool y_can_step;
  volatile bool z_can_step;
  volatile bool e_can_step;
  volatile bool f_can_step;

// Variables for acceleration calculations

  volatile long total_steps;            // The number of steps to take along the longest movement axis
  
  long timestep;               // microseconds
  bool nullmove;               // this move is zero length
  volatile bool real_move;     // Flag to know if we've changed something physical
  volatile bool live;          // Flag for when we're plotting a line

// Internal functions that need not concern the user

  // Take a single step

  void do_x_step();               
  void do_y_step();
  void do_z_step();
  void do_e_step();
  
  // Can this axis step?
  
  bool can_step(int min_pin, int max_pin, long current, long target, bool dir);
  
  // Read a limit switch
  
  bool read_switch(byte pin);
  
  // Work out the number of microseconds between steps
  
  long calculate_feedrate_delay(const float& feedrate);
  
  // Switch the steppers on and off
  
  void enable_steppers();
  void disable_steppers();
  
  // Custom short delay function (microseconds)
  
  //void delayMicrosecondsInterruptible(unsigned int us);
  
  
public:

  cartesian_dda();
  
  // Set where I'm going
  
  void set_target(const FloatPoint& p);
  
  // Start the DDA
  
  void dda_start();
  
  // Do one step of the DDA
  
  void dda_step();
  
  // Are we running at the moment?
  
  bool active();
  
  // True for mm; false for inches
  
  void set_units(bool using_mm);
  
  // Kill - stop all activity and turn off steppers
  
  void kill();
  
  // Record the selection of a new extruder
  
  //void set_extruder(extruder* ex);
};

// Short functions inline to save memory; particularly useful in the Arduino


//inline void cartesian_dda::set_extruder(extruder* ex)
//{
//  ext = ex;
//}

inline bool cartesian_dda::active()
{
  return live;
}

inline void cartesian_dda::do_x_step()
{
	digitalWrite(X_STEP_PIN, HIGH);
	delayMicrosecondsInterruptible(5);
	digitalWrite(X_STEP_PIN, LOW);
}

inline void cartesian_dda::do_y_step()
{
	digitalWrite(Y_STEP_PIN, HIGH);
	delayMicrosecondsInterruptible(5);
	digitalWrite(Y_STEP_PIN, LOW);
}

inline void cartesian_dda::do_z_step()
{
	digitalWrite(Z_STEP_PIN, HIGH);
	delayMicrosecondsInterruptible(5);
	digitalWrite(Z_STEP_PIN, LOW);
}

inline void cartesian_dda::do_e_step()
{
        ex[extruder_in_use]->sStep();
}

inline long cartesian_dda::calculate_feedrate_delay(const float& feedrate)
{  
        
	// Calculate delay between steps in microseconds.  Here it is in English:
        // (feedrate is in mm/minute, distance is in mm)
	// 60000000.0*distance/feedrate  = move duration in microseconds
	// move duration/total_steps = time between steps for master axis.

	return round( (distance*60000000.0) / (feedrate*(float)total_steps) );	
}

inline bool cartesian_dda::read_switch(byte pin)
{
	//dual read as crude debounce

	#if ENDSTOPS_INVERTING == 1
		return !digitalRead(pin) && !digitalRead(pin);
	#else
		return digitalRead(pin) && digitalRead(pin);
	#endif
}

#endif
