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
package org.safs.tools.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;


/**
 * Derived from RobotKeyEvent for inputting NLS characters. It holds an 'non-standard' string and will copy it to 
 * system Clipboard when doEvent called; a group of RobotKeyEvent(Menu+v) followed shall paste the content 
 * in system Clipboard to focused control.  
 * 
 * @author JunwuMa Sept 22, 2008
 * @see InputKeysParser
 * @see org.safs.robot.Robot
 * @see org.safs.tools.input.RobotKeyEvent
 * 
 * <br> 30 OCT, 2012 	(LeiWang)	Convert for android system.
 * <br> 21 FEB, 2013 	(LeiWang)	Modify method doEvent(): catch Throwable instead of Exception.
 */
public class RobotClipboardPasteEvent extends RobotKeyEvent{
	private String nlstring = null;
	private Vector<RobotKeyEvent> pasteKeys = null; 
	
	/** The constructor of class RobotClipboardPasteEvent. 
	 * @param content: a 'non-standard' string to be copied to Clipboard
	 * @param pasteKeys: key strokes for doing paste-operation. (Menu+v) 
	 */
	public RobotClipboardPasteEvent(String content, Vector<RobotKeyEvent> pasteKeys) {
		super(0, 0);
		this.nlstring = content;
		this.pasteKeys = pasteKeys;
	}
	/** Overides RobotKeyEvent.doEvent(Robot, int). 
	* Suggest ms_delay>=50, for paste operation needs to wait until copying Clipboard finished.
	*/
	public void doEvent(final Instrumentation inst, int ms_delay){
		final List<Boolean> success = new ArrayList<Boolean>();
		
		Log.d("RobotClipboardPasteEvent", "RobotClipboardPasteEvent: copying " + nlstring.toString() + " to system Clipboard");
		if(this.pasteKeys == null){
			Log.d("RobotClipboardPasteEvent", " .....Null pasteKeys in RobotClipboardPasteEvent. No paste operation.");
			return;
		}

		//Copy the nls string to the clipboard
		inst.runOnMainSync(new Runnable(){
			public void run() {
				try{
//					ClipboardManager clipboard = (ClipboardManager) inst.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) inst.getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
					android.content.ClipData clip = android.content.ClipData.newPlainText("NLS String", nlstring);
					clipboard.setPrimaryClip(clip);
					success.add(new Boolean(true));
					//ClipboardManager exists ONLY after Android-API11
					//for previous version, NoClassDefFoundError (is not subclass of Exception) will be thrown out
					//Catch Throwable instead of Exception
				} catch (Throwable e) {
					Log.d("RobotClipboardPasteEvent", " Met Exception "+ e.getClass().getSimpleName() + ":"+ e.getMessage());
					success.add(new Boolean(false));
				}
			}
		});
	    
		//Paste the clipboard's content to the destination object
		if(success.get(0).booleanValue()){
			Log.d("RobotClipboardPasteEvent", " .....delaying "+ms_delay+ "ms to paste(Menu+v) from Clipboard.");
			RobotKeyEvent.doKeystrokes(pasteKeys, inst, ms_delay);
		}
	}
}
