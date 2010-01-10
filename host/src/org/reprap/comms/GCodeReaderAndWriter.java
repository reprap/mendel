package org.reprap.comms;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import org.reprap.utilities.Debug;
import org.reprap.utilities.ExtensionFileFilter;
import org.reprap.Main;
import org.reprap.Preferences;
import org.reprap.geometry.LayerRules;

public class GCodeReaderAndWriter
{
	/**
	 * Stop sending a file (if we are).
	 */
	private boolean paused = false;
	
	/**
	 * Not quite sure why this is needed...
	 */
	private boolean alreadyReversed = false;
	
	/**
	 * The name of the port talking to the RepRap machine
	 */
	String portName;
	
	/**
	* this is if we need to talk over serial
	*/
	private SerialPort port;
	
	/**
	 * Flag to tell it we've finished
	 */
	private boolean exhaustBuffer = false;
	
	/**
	* this is for doing easy serial writes
	*/
	private PrintStream serialOutStream = null;
	
	/**
	 * this is our read handle on the serial port
	 */
	private InputStream serialInStream = null;
	
	/**
	 * The root file name for output (without ".gcode" on the end)
	 */
	private String opFileName;
	
	/**
	 * List of file names - used to reverse layer order when layers are done top-down
	 */
	private String[] opFileArray;
	
	/**
	 * Index into opFileArray
	 */
	private int opFileIndex;
	
	/**
	 * How does the first file name in a multiple set end?
	 */
	private static final String gcodeExtension = ".gcode";
	
	/**
	 * How does the first file name in a multiple set end?
	 */
	private static final String firstEnding = "_prologue";
	
	/**
	 * How does the last file name in a multiple set end?
	 */
	private static final String lastEnding = "_epilogue";
	
	/**
	 * Flag for temporary files
	 */
	private static final String tmpString = "_TeMpOrArY_";
		
	/**
	 * This is used for file input
	 */
	private BufferedReader fileInStream = null;
	
	/**
	 * How long is our G-Code input file (if any).
	 */
	private long fileInStreamLength = -1;
	
	/**
	 * This is for file output
	 */
	private PrintStream fileOutStream = null;
	
	/**
	 * The ring buffer that stores the commands for direct
	 * transmission to the RepRap machine.
	 */
	private int head, tail;
	private static final int buflen = 10; // No too long, or pause doesn't work well
	private String[] ringBuffer;
	
	/**
	 * The transmission to the RepRap machine is handled by
	 * a separate thread.  These control that.
	 */
	//private boolean threadLock = false;
	private Thread bufferThread = null;
	private int myPriority;
	
	/**
	 * Some commands (at the moment just M105 - get temperature) generate
	 * a response.  Return that as a string.
	 */
	private int responsesExpected = 0;
	private boolean responseAvailable = false;
	private String response;
	
	//private boolean sendFileToMachine = false;
		
	public GCodeReaderAndWriter()
	{
		paused = false;
		alreadyReversed = false;
		ringBuffer = new String[buflen];
		head = 0;
		tail = 0;
		//threadLock = false;
		exhaustBuffer = false;
		responsesExpected = 0;
		responseAvailable = false;
		response = "0000";
		opFileIndex = -1;
		try
		{
			portName = Preferences.loadGlobalString("Port(name)");
		} catch (Exception ex)
		{
			System.err.println("Cannot load preference Port(name).");
			portName = "stdout";
		}

		openSerialConnection(portName);

		myPriority = Thread.currentThread().getPriority();

		bufferThread = null;
		
//		if(serialOutStream != null)
//		{
//			bufferThread = new Thread() 
//			{
//				public void run() 
//				{
//					Thread.currentThread().setName("GCodeWriter() Buffer Thread");
//					bufferDeQueue();
//				}
//			};
//
//			bufferThread.start();
//		}
	}

	
	public boolean buildingFromFile()
	{
		return fileInStream != null;
	}
	
	public boolean savingToFile()
	{
		return fileOutStream != null;
	}

	/**
	 * Stop the printer building.
	 * This _shouldn't_ also stop it being controlled interactively.
	 */
	public void pause()
	{
		paused = true;
		while(!bufferEmpty())
		{
			//System.err.println("Waiting for buffer to empty.");
			sleep (131);
		}
	}
	
	/**
	 * Resume building.
	 *
	 */
	public void resume()
	{
		paused = false;
	}
	
	/**
	 * Start the production run
	 * (as opposed to driving the machine interactively).
	 */
	public void startRun()
	{
		if(fileOutStream != null)
		{
			// Exhause buffer before we start.
			if(bufferThread != null)
			{
				exhaustBuffer = true;
				while(exhaustBuffer) sleep(200);
			}
			bufferThread = null;
			head = 0;
			tail = 0;
		}	
	}
	
	/**
	 * Send a GCode file to the machine if that's what we have to do, and
	 * return true.  Otherwise return false.
	 *
	 */
	public boolean filePlay()
	{
		if(fileInStream == null)
		{
			// Not playing a file...
			return false;
		}
		
//		if(bufferThread == null)
//		{
//			System.err.println("GCodeWriter: attempt to write to non-existent buffer.");
//			return true;
//		}			
		
		// Launch a thread to run through the file, so we can return control
		// to the user
		
		Thread playFile = new Thread() 
		{
			public void run() 
			{
				Thread.currentThread().setName("GCode file printer");
				String line;
				long bytes = 0;
				double fractionDone = 0;
				try 
				{
					while ((line = fileInStream.readLine()) != null) 
					{
						bufferQueue(line);
						bytes += line.length();
						fractionDone = (double)bytes/(double)fileInStreamLength;
						setFractionDone(fractionDone);
						while(paused)
						{
							//System.err.println("Waiting for pause to end.");
							sleep(239);
						}
					}
					fileInStream.close();
				} catch (Exception e) 
				{  
					System.err.println("Error printing file: " + e.toString());
				}
			}
		};
		
		playFile.start();

	    return true;
	}
	
	public void setFractionDone(double fractionDone)
	{
		org.reprap.gui.botConsole.BotConsoleFrame.getBotConsoleFrame().setFractionDone(fractionDone);
	}
	
	/**
	 * Wrapper for Thread.sleep()
	 * @param millis
	 */
	public void sleep(int millis)
	{
		try
		{
			Thread.sleep(millis);
		} catch (Exception ex)
		{}		
	}
	
	/**
	 * All done.
	 *
	 */
	public void finish()
	{
		Debug.d("disposing of GCodeReaderAndWriter.");
		
//		// Wait for the ring buffer to be exhausted
//		if(fileOutStream == null && bufferThread != null)
//		{
//			exhaustBuffer = true;
//			while(exhaustBuffer) sleep(200);
//		}
		
		try
		{
			if (serialInStream != null)
				serialInStream.close();

			if (serialOutStream != null)
				serialOutStream.close();
			
			if (fileInStream != null)
				fileInStream.close();
			
			if (fileOutStream != null)
				fileOutStream.close();
			
		} catch (Exception e) {}
		
	}
	
	/**
	 * Anything in the buffer?  (NB this still works if we aren't
	 * using the buffer as then head == tail == 0 always).
	 * @return
	 */
	public boolean bufferEmpty()
	{
		return head == tail;
	}
	
	/**
	 * Between layers nothing will be queued.  Use the next two
	 * functions to slow and speed the buffer's spinning.
	 *
	 */
	public void slowBufferThread()
	{
		if(bufferThread != null)
			bufferThread.setPriority(1);
	}
	
	public void speedBufferThread()
	{
		if(bufferThread != null)		
			bufferThread.setPriority(myPriority);
	}	
	
	/**
	 * Queue a command into the ring buffer.  Note the use of prime time periods
	 * in the following code.  That's probably just superstition, but it feels more robust...
	 * @param cmd
	 */
	private void bufferQueue(String cmd)
	{
		if(serialOutStream == null)
		{
			Debug.d("bufferQueue: attempt to queue: " + cmd + " to a non-running output buffer.");
			return;
		}
		int com = cmd.indexOf(';');
		if(com > 0)
			cmd = cmd.substring(0, com);
		if(com != 0)
		{
			cmd = cmd.trim();
			if(cmd.length() > 0)
			{
				serialOutStream.print(cmd + "\n");
				// Message has effectively gone to the machine, so we can release the queuing thread
				//threadLock = false;				
				serialOutStream.flush();
				//oneSent = true;
				Debug.c("G-code: " + cmd + " dequeued and sent");
				// Wait for the machine to respond before we send the next command
				waitForOK();
			}
			return;
		}
	
		Debug.c("G-code: " + cmd + " not sent");
		
//		// Is the output thread running?
//		if(bufferThread == null)
//		{
//			Debug.d("bufferQueue: attempt to queue: " + cmd + " to a non-running output buffer.");
//			return;
//		}
//		// Are we locked out by the transmit thread?
//		//while(threadLock) sleep(5);
//		// Lock out the transmit thread
//		//threadLock = true;
//		// Next location in the ring
//		
//		int newHead = head + 1;
//		while(newHead == tail-1 || (tail == 0 && newHead == buflen-1))
//		{
//			// Release the lock so the transmit thread can get rid of stuff
//			//threadLock = false;
//			sleep(223);
//		}
//		synchronized(this)
//		{
//			//head++;
//			head = newHead;
//			if(head >= buflen) head = 0;
//			// Have we collided with the tail (i.e. is the ring full)?
//			//while(head == tail-1 || (tail == 0 && head == buflen-1))
//			//{
//			// Release the lock so the transmit thread can get rid of stuff
//			//threadLock = false;
//			//sleep(223);
//			//}
//			// Record the command in the buffer
//			ringBuffer[head] = cmd;
//			//threadLock = false;
//			Debug.c("G-code: " + cmd + " queued");
//		}
	}
	
//	/**
//	 * Loop getting the next thing in the buffer and transmitting it 
//	 * (or waiting for something to send if there's nothing there).
//	 *
//	 */
//	private void bufferDeQueue()
//	{
//		for(;;)
//		{
//			// Are we locked out by the queuing thread?
//			//while(threadLock) sleep(7);
//			// Wait for something to be there to send
//			while(bufferEmpty())
//			{
//				// If nothing more is ever coming, finish
//				if(exhaustBuffer)
//				{
//					exhaustBuffer = false;
//					return;
//				}
//				sleep(19);
//			}
//			// Lock out the queuing thread
//			//threadLock = true;
//			boolean oneSent = false;
//			synchronized(this)
//			{
//				// Pick up the next command in the buffer
//				tail++;
//				if(tail >= buflen) tail = 0;
//				// Strip any comment and send the command to the machine
//				String cmd = ringBuffer[tail];
//				int com = cmd.indexOf(';');
//				if(com > 0)
//					cmd = cmd.substring(0, com);
//				if(com != 0)
//				{
//					cmd = cmd.trim();
//					serialOutStream.print(cmd + "\n");
//					// Message has effectively gone to the machine, so we can release the queuing thread
//					//threadLock = false;				
//					serialOutStream.flush();
//					oneSent = true;
//					Debug.c("G-code: " + cmd + " dequeued and sent");
//					// Wait for the machine to respond before we send the next command
//					//waitForOK();
//				} else
//					Debug.c("G-code: " + ringBuffer[tail] + " not sent");
//				// Just for safety
//				//threadLock = false;
//			}
//			if(oneSent)
//				waitForOK();
//		}
//	}

	/**
	 * Wait for the GCode interpreter in the RepRap machine to send back "ok\n".
	 *
	 */
	private void waitForOK()
	{
		int i, count;
		String resp = "";
		count = 0;
		
		for(;;)
		{
			try
			{
				i = serialInStream.read();
			} catch (Exception e)
			{
				i = -1;
			}

			//anything found?
			if (i >= 0)
			{
				char c = (char)i;

				//is it at the end of the line?
				if (c == '\n' || c == '\r')
				{
					if (resp.startsWith("ok"))
					{
						if(resp.length() > 2)
							Debug.c("GCode acknowledged with message: " + resp);
						else
							Debug.c("GCode acknowledged");
						return;
					} else if (resp.startsWith("T:"))
					{
						Debug.c("GCodeWriter.waitForOK() - temperature reading: " + resp);
						if(responsesExpected > 0)
						{
							response = resp;
							responseAvailable = true;
						} else
							System.err.println("GCodeWriter.waitForOK(): temperature response returned when none expected.");
					} else if (resp.startsWith("E:"))
					{
						System.err.println("GCodeWriter.waitForOK(): temperature error returned: " + resp);
					} 
					else if (resp.startsWith("start") || resp.contentEquals(""))
					{	
						// That was the reset string from the machine or a null line; ignore it.
					}else
					{
						//Gone wrong.  Start again.
						Debug.c("GCodeWriter.waitForOK() dud response: " + resp);
						count++;
						if(count >= 3)
						{
							System.err.println("GCodeWriter.waitForOK(): try count exceeded.  Last line received was: " + resp);
							return;
						}
					}
					// If we get here we need a new string
					resp = "";
				} else
					resp += c;
			}
		}
	}
	
	/**
	 * Send a G-code command to the machine or into a file.
	 * @param cmd
	 */
	public void queue(String cmd)
	{
		//trim it and cleanup.
		cmd = cmd.trim();
		cmd = cmd.replaceAll("  ", " ");
		
		if(fileOutStream != null)
		{
			fileOutStream.println(cmd);
			Debug.c("G-code: " + cmd + " written to file");
		} else
			bufferQueue(cmd);
	}
	
	/**
	 * Send a G-code command to the machine and return
	 * a response.
	 * @param cmd
	 */
	public String queueRespond(String cmd)
	{
		if(serialOutStream == null)
		{
			Debug.d("queueRespond: attempt to queue: " + cmd + " to a non-running output buffer.");
			return "0000";
		}
		//trim it and cleanup.
		cmd = cmd.trim();
		cmd = cmd.replaceAll("  ", " ");
		
		if (fileOutStream != null)
		{
			System.err.println("GCodeWriter.queueRespond() called when file being created.");
			return "0000"; // Safest compromise
		}
		responsesExpected++;
		bufferQueue(cmd);
		if(responsesExpected <= 0)
		{
			System.err.println("GCodeWriter.getResponse() called when no response expected.");
			responsesExpected = 0;
			responseAvailable = false;
			return "0000";
		}
		while(!responseAvailable) sleep(31);
		responseAvailable = false;
		responsesExpected--;
		return response;		
	}
	

	private void openSerialConnection(String portName)
	{
		
		int baudRate = 19200;
		serialInStream = null;
		serialOutStream = null;
		
		//open our port.
		Debug.d("GCode opening port " + portName);
		Main.setRepRapPresent(false);
		try 
		{
			CommPortIdentifier commId = CommPortIdentifier.getPortIdentifier(portName);
			port = (SerialPort)commId.open(portName, 30000);
		} catch (NoSuchPortException e) {
			System.err.println("Error opening port: " + portName);
			return;
		}
		catch (PortInUseException e){
			System.err.println("Port '" + portName + "' is already in use.");
			return;			
		}
		Main.setRepRapPresent(true);		
		//get our baudrate
		try {
			baudRate = Preferences.loadGlobalInt("BaudRate");
		}
		catch (IOException e){}
		
		// Workround for javax.comm bug.
		// See http://forum.java.sun.com/thread.jspa?threadID=673793
		// FIXME: jvandewiel: is this workaround also needed when using the RXTX library?
		try {
			port.setSerialPortParams(baudRate,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		}
		catch (UnsupportedCommOperationException e) {
			Debug.d("An unsupported comms operation was encountered.");
			return;		
		}

/*			 
		port.setSerialPortParams(baudRate,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
*/		
		// End of workround
		
		try {
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		} catch (Exception e) {
			// Um, Linux USB ports don't do this. What can I do about it?
		}
		
		try {
			port.enableReceiveTimeout(1);
		} catch (UnsupportedCommOperationException e) {
			Debug.d("Read timeouts unsupported on this platform");
		}

		//create our steams
		try {
			OutputStream writeStream = port.getOutputStream();
			serialInStream = port.getInputStream();
			serialOutStream = new PrintStream(writeStream);
		} catch (IOException e) {
			System.err.println("GCodeWriter: Error opening serial port stream.");
			serialInStream = null;
			serialOutStream = null;
			return;		
		}

		//arduino bootloader skip.
		Debug.d("Attempting to initialize Arduino/Sanguino");
        try {Thread.sleep(1000);} catch (Exception e) {}
        for(int i = 0; i < 10; i++)
                serialOutStream.write('0');
        try {Thread.sleep(1000);} catch (Exception e) {}
        
        return;
	}
	

	
	public String loadGCodeFileForMaking()
	{
		JFileChooser chooser = new JFileChooser();
        FileFilter filter;
        filter = new ExtensionFileFilter("G Code file to be read", new String[] { "gcode" });
        chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		//chooser.setCurrentDirectory();

		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			String name = chooser.getSelectedFile().getAbsolutePath();
			try
			{
				Debug.d("opening: " + name);
				fileInStreamLength = chooser.getSelectedFile().length();
				fileInStream = new BufferedReader(new FileReader(chooser.getSelectedFile()));
				return chooser.getSelectedFile().getName();
			} catch (FileNotFoundException e) 
			{
				System.err.println("Can't read file " + name);
				fileInStream = null;
				return null;
			}
		} else
		{
			System.err.println("Can't read file.");
			fileInStream = null;
		}

		return null;
	}
	
	public String setGCodeFileForOutput(boolean topDown, String fileRoot)
	{

		File defaultFile = new File(fileRoot + ".gcode");
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(defaultFile);
		FileFilter filter;
		filter = new ExtensionFileFilter("G Code file to write to", new String[] { "gcode" });
		chooser.setFileFilter(filter);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		opFileName = null;
		opFileArray = null;
		opFileIndex = -1;
		int result = chooser.showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			opFileName = chooser.getSelectedFile().getAbsolutePath();
			if(opFileName.endsWith(gcodeExtension))
				opFileName = opFileName.substring(0, opFileName.length() - 6);

			try
			{
				boolean doe = false;
				String fn = opFileName;
				if(topDown)
				{
					opFileIndex = 0;
					fn += firstEnding;
					fn += tmpString;
					doe = true;
				}
				fn += gcodeExtension;
				
				Debug.d("opening: " + fn);
				File fl = new File(fn);
				if(doe) fl.deleteOnExit();
				FileOutputStream fileStream = new FileOutputStream(fl);
				fileOutStream = new PrintStream(fileStream);
				String shortName = chooser.getSelectedFile().getName();
				if(!shortName.endsWith(gcodeExtension))
					shortName += gcodeExtension;
				return shortName;
			} catch (FileNotFoundException e) 
			{
				opFileArray = null;
				opFileIndex = -1;
				System.err.println("Can't write to file '" + opFileName);
				opFileName = null;
				fileOutStream = null;
			}
		}
		else
		{
			fileOutStream = null;
		}
		return null;
	}
	
	public void startingLayer(LayerRules lc)
	{
		// If no filename or the index is not set, forget about the start layer. - Vik, 23-Feb-2009
		if((opFileIndex < 0)  || (opFileName == null))
			return;
		
		if(opFileArray == null)
		{
			if(lc.getTopDown())
			{
				opFileIndex = 0;
				opFileArray = new String[lc.getMachineLayerMax() + 3];
				opFileArray[opFileIndex] = opFileName + firstEnding + tmpString + gcodeExtension;
				finishedLayer();
			}
		}
		
		opFileArray[opFileIndex] = opFileName + lc.getMachineLayer() + tmpString + gcodeExtension;
		try
		{
			File fl = new File(opFileArray[opFileIndex]);
			fl.deleteOnExit();
			FileOutputStream fileStream = new FileOutputStream(fl);
			fileOutStream = new PrintStream(fileStream);
		} catch (Exception e)
		{
			System.err.println("Can't write to file " + opFileArray[opFileIndex]);
		}
	}
	
	public void startingEpilogue()
	{
		if(opFileArray == null)
			return;
		opFileArray[opFileIndex] = opFileName + lastEnding + tmpString + gcodeExtension;
		try
		{
			File fl = new File(opFileArray[opFileIndex]);
			fl.deleteOnExit();
			FileOutputStream fileStream = new FileOutputStream(fl);
			fileOutStream = new PrintStream(fileStream);
		} catch (Exception e)
		{
			System.err.println("Can't write to file " + opFileArray[opFileIndex]);
		}
	}
	
	public void finishedLayer()
	{
		if(opFileArray == null)
			return;
		fileOutStream.close();
		opFileIndex++;
	}
	
	private void copyFile(PrintStream ps, String ip)
	{
		File f = null;
		try 
		{
			f = new File(ip);
			FileReader fr = new FileReader(f);
			int character;
			while ((character = fr.read()) >= 0) 
				ps.print((char)character);
			
			ps.flush();
			fr.close();
		} catch (Exception e) 
		{  
			System.err.println("Error copying file: " + e.toString());
		}
	}
	
	public void reverseLayers()
	{
		if(opFileArray == null || alreadyReversed)
			return;
		
		// Stop this being called twice...
		
		alreadyReversed = true;
		
		try
		{
			FileOutputStream fileStream = new FileOutputStream(opFileName + gcodeExtension);
			fileOutStream = new PrintStream(fileStream);
		} catch (Exception e)
		{
			System.err.println("Can't write to file " + opFileName + gcodeExtension);
		}
		
		copyFile(fileOutStream, opFileArray[0]);
		for(int i = opFileIndex - 1; i > 0; i--)
			copyFile(fileOutStream, opFileArray[i]);
		copyFile(fileOutStream, opFileArray[opFileIndex]);
	}
}