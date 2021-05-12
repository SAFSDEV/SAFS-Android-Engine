/**
 * Copyright (C) SAS Institute, All rights reserved.
 * General Public License: https://www.gnu.org/licenses/gpl-3.0.en.html
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/
package org.safs.robot;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.safs.android.engine.R;
import org.safs.text.INIFileReader;
import org.safs.tools.input.CreateUnicodeMap;
import org.safs.tools.input.InputKeysParser;
import org.safs.tools.input.RobotClipboardPasteEvent;
import org.safs.tools.input.RobotKeyEvent;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Utility functions for common user interactions on the system.
 * 
 * @author CarlNagle Sept 09, 2008
 * @see java.awt.Robot
 * @see org.safs.tools.input.CreateUnicodeMap
 * @see org.safs.tools.input.InputKeysParser
 * 
 * <br> JunwuMa SEP 23, 2008  Added doEvents(Robot, Vector) running RobotClipboardPasteEvent with proper delay time
 *                            for ctrl+v(paste) job done. 
 * <br> CarlNagle  MAR 25, 2009  Added MouseDrag support
 * <br> CarlNagle  APR 03, 2009  Enhance MouseDrag support to work for more apps.
 * <br> LeiWang JUL 04, 2011  Add methods to maximize, minimize, restore, close window by key-mnemonic.
 * <br> LeiWang  OCT 30, 2012  Convert for Android's system.
 * <br> LeiWang  NOV 06, 2012  Add methods to handle the Android's Clipboard.
 */
public class Robot {
	public String TAG = "Robot";
	
	Instrumentation instrument = null;
	InputKeysParser keysparser = null;
	
	public Robot(Instrumentation instrument){
		this.instrument = instrument;
	}
	
	public void setInstrument(Instrumentation instrument){
		this.instrument = instrument;
	}
	
	public void debug(String message){
		System.out.println(message);
	}
	
	/**
	 * Retrieve the active InputKeysParser.
	 * If one does not yet exist the routine will attempt to instantiate it.
	 * @return InputKeysParser or null
	 * @see org.safs.tools.input.InputKeysParser
	 */
	public InputKeysParser getInputKeysParser(){
		if(keysparser==null){
			InputStream stream = null;
			try{
				stream = readRawFile(CreateUnicodeMap.DEFAULT_FILE);
				INIFileReader reader = new INIFileReader(stream, 0, false);
				debug(TAG+" SAFS Robot InputKeysParser initialization: "+ reader);
				keysparser = new InputKeysParser(reader);
				debug(TAG+" SAFS Robot InputKeysParser: "+ keysparser);
			}catch(Exception e){
				debug(TAG+" SAFS Robot.getInputKeysParser: Met "+ e.getClass().getSimpleName()+":"+e.getMessage());
			}
		}
		return keysparser;
	}

	/**
	 * @param filenameWithoutSuffix, String, the name of file stored in project folder res/raw
	 * 
	 * @return	InputStream, the InputStream of the file.
	 */
	public InputStream readRawFile(String filenameWithoutSuffix){
		Field field = null;
		InputStream in = null;
		
		try {
			field = R.raw.class.getDeclaredField(filenameWithoutSuffix);
			if(field != null && instrument!=null){
				debug(TAG+" loading raw file: "+filenameWithoutSuffix);
				debug(TAG+" context= "+instrument.getContext());
				debug(TAG+" getResources= "+instrument.getContext().getResources());
				in = instrument.getContext().getResources().openRawResource(field.getInt(R.raw.class));
				if(in!=null) debug(TAG+" got InputStream. ");
				else debug(TAG+" InputStream is null!!! ");
			}
		} catch (Exception e) {
			debug(TAG+" Met "+e.getClass().getSimpleName()+": "+e.getMessage());
		}
		
		return in;
	}
		
	/**
	 * Type keyboard input.
	 * The input goes to the current keyboard focus target.  The String input 
	 * can include all special characters and processing as documented in the  
	 * InputKeysParser class.
	 * 
	 * @param input -- the String of characters to enter.
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 * 
	 * @see org.safs.tools.input.InputKeysParser
	 */
	public Object inputKeys(String input){
	   	debug("SAFS Robot processing InputKeys: "+ input);
	   	InputKeysParser parser = getInputKeysParser();
	   	try{
	   		Vector<RobotKeyEvent> keystrokes = parser.parseInput(input);
	   		doEvents(instrument, keystrokes);
	   		return new Boolean(true);
	   	}catch(Throwable e){
	   		debug(TAG+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
	   		return new Boolean(false);
	   	}
	}
	
	/**
	 * Type keyboard input characters unmodified.  No special key processing.
	 * The input goes to the current keyboard focus target.  The String input 
	 * will be treated simply as literal text and typed as-is.
	 * 
	 * @param input -- the String of characters to enter.
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 * 
	 * @see org.safs.tools.input.InputKeysParser
	 */
	public Object inputChars(String input){
		debug("SAFS Robot processing InputKeys: "+ input);
	   	InputKeysParser parser = getInputKeysParser();
	   	try{
	   		Vector<RobotKeyEvent> keystrokes = parser.parseChars(input);
	   		doEvents(instrument, keystrokes);
	   		return new Boolean(true);
	   	}catch(Throwable e){
	   		debug(TAG+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
	   		return new Boolean(false);
	   	}
	}
	
	private void doEvents(Instrumentation bot, Vector<RobotKeyEvent> RobotKeys){
	   	if(bot == null)
	   		return;
		Iterator<RobotKeyEvent> events = RobotKeys.iterator();
	   	Object event;	   	
	   	while(events.hasNext()){
   			event = events.next();
   			if(event instanceof RobotClipboardPasteEvent)
   				//setting ms_delay=50, for the Paste from Clipboard needs to wait until copying Clipboard finished.
   				((RobotClipboardPasteEvent)event).doEvent(bot, 50);
   			else
   				((RobotKeyEvent)event).doEvent(bot, 0);	   	
   		}		
	}
	/**
	 * Workhorse Click routine.  
	 * Allows us to Click--Press & Release--any combination of MotionEvent.BUTTON_XXX 
	 * any number of times. 
	 * 
	 * @param x screen X coordinate
	 * @param y screen Y coordinate
	 * @param buttonmask -- specific MotionEvent.BUTTON_XXX 
	 * @param nclicks -- number of times to click (press and release)
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 * 
	 * @see java.awt.Robot#mousePress(int)
	 */
	public Object click(int x, int y, int buttonmask, int nclicks){
		debug("Robot click at:"+ x +","+ y +" using button mask "+ buttonmask +" "+ nclicks +" times.");
	   	try{
	   		for(int i = 0; i < nclicks; i++){
	   			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
	   					MotionEvent.ACTION_DOWN|buttonmask, x, y, 0));
	   			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
	   					MotionEvent.ACTION_UP|buttonmask, x, y, 0));
	   		}
	   		return new Boolean(true);
	   	}catch(Exception e){
	   		debug(TAG+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
	   		return new Boolean(false);
	   	}
	}
	
	/**
	 * Workhorse Click with Keypress routine.  
	 * Allows us to Click--Press & Release--any combination of MotionEvent.BUTTON_XXX 
	 * any number of times with a single Key Press & Release. 
	 * 
	 * @param x screen X coordinate
	 * @param y screen Y coordinate
	 * @param buttonmask -- specific MotionEvent.BUTTON_XXX 
	 * @param keycode -- specific keycode to press & release. Ex: KeyEvent.VK_SHIFT 
	 * @param nclicks -- number of times to click (press and release)
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 * 
	 * @see MotionEvent.BUTTON_XXX
	 */
	public Object clickWithKeyPress(int x, int y, int buttonmask, int keycode, int nclicks){
		debug("Robot click at:"+ x +","+ y +" using button mask "+ buttonmask +", keycode "+ keycode +", "+ nclicks +" times.");
	   	try{
	   		instrument.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
	   		for(int i = 0; i < nclicks; i++){
	   			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
	   					MotionEvent.ACTION_DOWN|buttonmask, x, y, 0));
	   			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
	   					MotionEvent.ACTION_UP|buttonmask, x, y, 0));
	   		}
	   		instrument.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, keycode));
	   		return new Boolean(true);
	   	}catch(Exception e){
	   		debug(TAG+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
	   		return new Boolean(false);
	   	}
	}
	

	/**
	 * Move the mouse cursor to the specified x,y coordinates then perform a 
	 * single mousePress and Release to execute a Click.
	 * @param x
	 * @param y
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 */
	public Object click(int x, int y){
		try{ return click(x,y,MotionEvent.BUTTON_PRIMARY,1); }
		catch(Throwable api14_and_below){
			return click(x,y,0,1); 
		}
	}

	
	/**
	 * Move the mouse cursor to the specified x,y coordinates then perform a 
	 * single mousePress and Release to execute a Click.
	 * @param x
	 * @param y
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 */
	public Object doubleClick(int x, int y){
		try{ return click(x,y,MotionEvent.BUTTON_PRIMARY,2); }
		catch(Throwable api14_and_below){
			return click(x,y,0,2);
		}
	}

	/**
	 * Move the mouse cursor to the specified x,y coordinates then perform a 
	 * single mousePress and Release to execute a RightClick.
	 * @param x
	 * @param y
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 */
	public Object rightClick(int x, int y){
		try{ return click(x,y,MotionEvent.BUTTON_SECONDARY,1);}
		catch(Throwable api14_and_below){
			return click(x,y,0,1); 
		}
	
	}

	
	/**
	 * Move the mouse cursor to the specified Point coordinates then perform a 
	 * single mousePress and Release to execute a Click.
	 * This routine simply calls click with the x,y coordinates in the Point.
	 * @param Point
	 * @return Object returned from click above.
	 * @see #click(int, int)
	 */
	public Object click(Point p){
		return click(p.x, p.y);
	}

	/**
	 * Move the mouse cursor to the specified Point coordinates then perform a 
	 * single mousePress and Release to execute a Click.
	 * This routine simply calls click with the x,y coordinates in the Point.
	 * @param Point
	 * @return Object returned from click above.
	 * @see #click(int, int)
	 */
	public Object rightClick(Point p){
		return rightClick(p.x, p.y);
	}

	/**
	 * Move the mouse cursor to the specified Point coordinates then perform a 
	 * single mousePress and Release to execute a Click.
	 * This routine simply calls click with the x,y coordinates in the Point.
	 * @param Point
	 * @return Object returned from click above.
	 * @see #click(int, int)
	 */
	public Object doubleClick(Point p){
		return doubleClick(p.x, p.y);
	}

	/**
	 * Move the mouse cursor to the specified start Point coordinates then perform a 
	 * single mousePress using buttonMasks and drag\move then Release the mouse button at the end point.
	 * @param Point screen coordinates to start of mouse press and drag.
	 * @param Point screen coordinates to end mouse drag and mouse release.
	 * @param int button masks to use during drag
	 * @return Object Currently we return a Boolean(true) object, but this may 
	 * be subject to change.
	 */
	public Object mouseDrag(Point start, Point end, int buttonMasks){
		debug("Robot mouseDrag from:"+ start +" to:"+ end +" using button mask "+ buttonMasks);
		
		try{
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_MOVE, start.x+1, start.y+1, 0));
			Thread.sleep(150);
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_MOVE, start.x, start.y, 0));
			Thread.sleep(400);
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_DOWN|buttonMasks, start.x+1, start.y+1, 0));
			Thread.sleep(400);
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_MOVE, end.x-1, end.y-1, 0));
			Thread.sleep(400);
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_MOVE, end.x, end.y, 0));
			Thread.sleep(400);
			instrument.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), 
					MotionEvent.ACTION_UP|buttonMasks, start.x+1, start.y+1, 0));
			return new Boolean(true);
		}catch(Exception e){
	   		debug(TAG+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
	   		return new Boolean(false);
	   	}
	}

	/**
	 * Move the mouse cursor to the specified start Point coordinates then perform a 
	 * single mousePress (Button1) and drag\move then Release the mouse button at the end point.
	 * @param Point screen coordinates to start of mouse press and drag.
	 * @param Point screen coordinates to end mouse drag and mouse release.
	 * @return Object returned from generic mouseDrag.
	 * @see #mouseDrag(Point,Point,int)
	 */
	public Object leftDrag(Point start, Point end){
		try{ return mouseDrag(start, end, MotionEvent.BUTTON_PRIMARY);}
		catch(Throwable api14_and_down){
			return mouseDrag(start, end, 0);
		}
	}

	/**
	 * Move the mouse cursor to the specified start Point coordinates then perform a 
	 * single mousePress (Button3) and drag\move then Release the mouse button at the end point.
	 * @param Point screen coordinates to start of mouse press and drag.
	 * @param Point screen coordinates to end mouse drag and mouse release.
	 * @return Object returned from generic mouseDrag.
	 */
	public Object rightDrag(Point start, Point end){
		try{ return mouseDrag(start, end, MotionEvent.BUTTON_SECONDARY); }
		catch(Throwable api14_and_down){
			return mouseDrag(start, end, 0);
		}
	}

	/**
	 * Move the mouse cursor to the specified start Point coordinates then perform a 
	 * single mousePress (Button2) and drag\move then Release the mouse button at the end point.
	 * @param Point screen coordinates to start of mouse press and drag.
	 * @param Point screen coordinates to end mouse drag and mouse release.
	 * @return Object returned from generic mouseDrag.
	 */
	public Object centerDrag(Point start, Point end){
		try{ return mouseDrag(start, end, MotionEvent.BUTTON_TERTIARY); }
		catch(Throwable api14_and_down){
			return mouseDrag(start, end, 0);
		}
	}
	
	/**
	 * Clear clip board, we just set the clipbard's content as a void string "".<br>
	 * 
	 * @return Object returned from clip board, null if there is no data.
	 */		
	public boolean clearClipboard(){
		return setClipboard("Plain String", "");
	}
	
	/**
	 * Set a string to clip board.<br>
	 * Android's clipboard can contain different kinds of object, for now we just handle<br>
	 * the plain text object.<br>
	 * 
	 * @param content String, the plain text to be set to clipboard.
	 * @return Object returned from clip board, null if there is no data.
	 */		
	public boolean setClipboard(final String description, final String content){
		final List<Boolean> success = new ArrayList<Boolean>();
		final String debugmsg = TAG+".setClipboard(): ";
		
		instrument.runOnMainSync(new Runnable() {
			public void run() {
				try {
//					ClipboardManager clipboard = (ClipboardManager) instrument.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) instrument.getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = android.content.ClipData.newPlainText(description, content);
					clipboard.setPrimaryClip(clip);
					success.add(new Boolean(true));
				} catch (Exception e) {
					debug(debugmsg + " Met Exception "+ e.getClass().getSimpleName() + ":"+ e.getMessage());
					success.add(new Boolean(false));
				}catch(Throwable api10_and_below){
					debug(debugmsg + " Met "+ api10_and_below.getClass().getSimpleName() + ":"+ api10_and_below.getMessage());
					success.add(new Boolean(false));
				}
			}
		});
		return success.get(0).booleanValue();
	}

	/**
	 * Get the Object from the clip board.<br>
	 * Android's clipboard can contain different kinds of object, for now we just handle<br>
	 * the plain text object.<br>
	 * 
	 * @return Object returned from clip board, null if there is no data.
	 */	
	public Object getClipboardContent(){
		final Vector<Object> tempContainer = new Vector<Object>();
		final String debugmsg = TAG+".getClipboardContent(): ";

		instrument.runOnMainSync(new Runnable(){
			public void run() {
				try{
					//Because we are now in the process of AUT, so the current uid is that of AUT.
					//When we call getPrimaryClip() of clipboard service, the current uid (AUT's) will be past to that method
					//and used to compare with the uid of the process of package contained in the context.
					//To make sure that the verification succeed, we need to choose the context of AUT
					//instrument.getContext(): give us the context of the Runner
					//instrument.getTargetContext(): give us the context of AUT
//					ClipboardManager clipboard = (ClipboardManager) instrument.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) instrument.getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
					if(clipboard.hasPrimaryClip()){
						android.content.ClipData data = clipboard.getPrimaryClip();
						String description = data.getDescription().getLabel().toString();
						int itemCount = data.getItemCount();
						debug(debugmsg+" getClipboardContent(): data's description: "+description);
						debug(debugmsg+" getClipboardContent(): data has "+itemCount+" items");
						//We take only the first object
						if(itemCount>0){
							tempContainer.add(data.getItemAt(0).getText().toString());
						}else{
							tempContainer.add(null);
						}
					}else{
						tempContainer.add(null);
					}
				}catch(Exception e){
					debug(debugmsg+" Met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
					tempContainer.add(null);
				}catch(Throwable api10_and_below){
					debug(debugmsg + " Met "+ api10_and_below.getClass().getSimpleName() + ":"+ api10_and_below.getMessage());
					tempContainer.add(null);
				}
			}
		});

		return tempContainer.get(0);
	}
	
//	/**
//	 * Minimize all windows by the short cut 'Windows+M'<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void minimizeAllWindows(){
//		java.awt.Robot robot = getRobot();
//		
//		robot.keyPress(KeyEvent.VK_WINDOWS);
//		robot.keyPress(KeyEvent.VK_M);
//		robot.keyRelease(KeyEvent.VK_M);
//		robot.keyRelease(KeyEvent.VK_WINDOWS);
//	}
//	
//	/**
//	 * <em>Pre-condition:</em> The window should be focused
//	 * Get the window system menu by short-cut 'Alt+Space'<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void getWindowSystemMenu(){
//		java.awt.Robot robot = getRobot();
//		
//		robot.keyPress(KeyEvent.VK_ALT);
//		robot.keyPress(KeyEvent.VK_SPACE);
//		robot.keyRelease(KeyEvent.VK_SPACE);
//		robot.keyRelease(KeyEvent.VK_ALT);
//	}
//	/**
//	 * <em>Pre-condition:</em> The window should be focused
//	 * Minimize window by mnemonic key 'n' of window-system-menu.<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void minimizeFocusedWindow(){
//		getWindowSystemMenu();
//		robot.keyPress(KeyEvent.VK_N);
//	}
//	/**
//	 * <em>Pre-condition:</em> The window should be focused
//	 * Minimize window by mnemonic key 'x' of window-system-menu.<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void maximizeFocusedWindow(){
//		getWindowSystemMenu();
//		robot.keyPress(KeyEvent.VK_X);
//	}
//	/**
//	 * <em>Pre-condition:</em> The window should be focused
//	 * Minimize window by mnemonic key 'R' of window-system-menu.<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void restoreFocusedWindow(){
//		getWindowSystemMenu();
//		robot.keyPress(KeyEvent.VK_R);
//	}
//	/**
//	 * <em>Pre-condition:</em> The window should be focused
//	 * Minimize window by mnemonic key 'C' of window-system-menu.<br>
//	 * This works only for windows system.
//	 * @throws AWTException
//	 */
//	public void closeFocusedWindow(){
//		getWindowSystemMenu();
//		robot.keyPress(KeyEvent.VK_C);
//	}
}
