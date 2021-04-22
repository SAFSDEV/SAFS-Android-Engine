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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.safs.text.CaseInsensitiveHashtable;
import org.safs.text.INIFileReader;

import android.util.Log;
import android.view.KeyEvent;

/**
 * @author CarlNagle FEB 13, 2007
 * @see java.awt.Robot
 * @see java.awt.event.KeyEvent
 * 
 * <br> SEP 22, 2008  (JunwuMa) Add NLS 'keyboard' input support. Actually it is not a REAL keyboard input support. 
 *                    Instead it uses an alternative way. NLS characters in an input string will NOT be translated 
 *                    to keystrokes in certain IME, for the reason of 1) too much code points in some Non-English 
 *                    languages (Chinese), 2)a NLS character owns different keystrokes in different IME and 3)keystrokes for a Chinese character relies 
 *                    on IME too much.
 * 					  Two steps for inputting NLS characters.
 *					  1.	Copy NLS characters to system Clipboard. Add an event: SetClipboardEvent.
 *                    2.	Add 'Ctrl+V' keyboard event to paste the content in system Clipboard to focused control.
 *                    Possible drawbacks:
 *                    Not sure if it can be used by controls that prevents 'Ctrl+V' from Window OS. If yes, a REAL unique hot 
 *                    key (Ctrl+Shift+V ?) may be introduced for doing so on OS level.
 * <br>	JAN 06, 2009  (LeiWang)	Modify method parseBraces(): Use standards instead of special to get character mapping.
 * 															 Otherwise {a 4} will not be treated as inputting 'aaaa' because special does not
 * 															 contains mapping of 'a'.
 * <br>	DEC 07, 2010  (LeiWang)	Add method antiParse(), parseKeyCode(), getReverseMap(): Convert a list of RobotKeyEvent to a SAFS's key string
 * <br> OCT 30, 2012  (LeiWang)	Convert for android system.
 */
public class InputKeysParser {

	private CharInfo alt = null;
	private CharInfo control = null;
	private CharInfo shift = null;
	private CharInfo enter = null;
	//TODO There is no keycode for '{' '}' '(' ')' in Android API,
	//But CharInfo needs a int keycode, we can just give a whatever int as we didn't
	//reaally use this keycode in this program
	private CharInfo brace_left = null;
	private CharInfo brace_right = null;
	private CharInfo paren_left = null;
	private CharInfo paren_right = null;
	
    private Hashtable tokens = new Hashtable();
	//specials and standards contain pair <charString , keycodeString>, got from SAFSKeycodeMap.dat
	private Hashtable specials = new CaseInsensitiveHashtable();
	private Hashtable standards = new Hashtable();
	//Two hashMaps to contain the reverse-pair <keycodeString , charString> of specials and standards
	private Map<String,String>   specialsKeyToChar = null;
	private Map<String,String>   standardsKeyToChar = null;

	protected boolean alt_on = false;
	protected boolean shift_on = false;
	protected boolean ctrl_on = false;

	protected static String SHIFT_END_DELETE = "+{END}{DEL}";
	protected static String SHIFT = "SHIFT";
	protected static String SHIFT_PLUS = "SHIFT+";
	protected static String END = "END";
	protected static String DEL = "DEL";
	protected static String QUOTE = "\"";
	protected static String SPACE = " ";

	private InputKeysParser(){
		alt = new CharInfo('%', KeyEvent.KEYCODE_ALT_LEFT);
		try{ control = new CharInfo('^', KeyEvent.KEYCODE_CTRL_LEFT);}
		catch(Throwable api10_and_below){ 
			// arbitrarily make CTRL a SPACE char until we know better
			control = new CharInfo('^',KeyEvent.KEYCODE_SPACE);
		}
		shift = new CharInfo('+', KeyEvent.KEYCODE_SHIFT_LEFT);
		enter = new CharInfo('~',KeyEvent.KEYCODE_ENTER);
		//TODO There is no keycode for '{' '}' '(' ')' in Android API,
		//But CharInfo needs a int keycode, we can just give a whatever int as we didn't
		//reaally use this keycode in this program
		brace_left = new CharInfo('{', KeyEvent.KEYCODE_LEFT_BRACKET);
		brace_right = new CharInfo('}', KeyEvent.KEYCODE_RIGHT_BRACKET);
		paren_left = new CharInfo('(', KeyEvent.KEYCODE_9);
		paren_right = new CharInfo(')', KeyEvent.KEYCODE_0);
		
	}
	public InputKeysParser(INIFileReader config){
		this();
		if (config==null) throw new IllegalArgumentException("INIFileReader must not be null!");
		//override default parsing tokens if necessary.
		setTokenInfo(config, alt, CreateUnicodeMap.ALT);
		setTokenInfo(config, control, CreateUnicodeMap.CONTROL);
		setTokenInfo(config, shift, CreateUnicodeMap.SHIFT);	
		setTokenInfo(config, enter, CreateUnicodeMap.ENTER);
		
		setTokenInfo(config, brace_left, CreateUnicodeMap.BRACELEFT);
		setTokenInfo(config, brace_right, CreateUnicodeMap.BRACERIGHT);
		setTokenInfo(config, paren_left, CreateUnicodeMap.PARENLEFT);
		setTokenInfo(config, paren_right, CreateUnicodeMap.PARENRIGHT);
		
		//load in all special strings
		setStringInfo(config, CreateUnicodeMap.SPECIAL, specials);
		
		//load in all standard strings
		setStringInfo(config, CreateUnicodeMap.STANDARD, standards);
		
		//create reverse pair of specials and standards
		specialsKeyToChar = getReverseMap(specials);
		standardsKeyToChar = getReverseMap(standards);
			
		//create common strings
		createCommonStrings();
	}
	
	/**
	 * <b>Purpose</b> Create a map containing reverse pair of the input map
	 * 
	 * @param map
	 * @return		A map containing reverse pair <value, key>
	 */
	private Map<String,String> getReverseMap(Map map){
		if(map==null) return new HashMap<String,String>();
		Set keys = map.keySet();
		if(keys==null) return new HashMap<String,String>();

		HashMap<String,String>   reverseMap = new HashMap<String,String>(30);
		Object key = null;
		Object value = null;
		Iterator iter = keys.iterator();
		while(iter.hasNext()){
			key = iter.next();
			value = map.get(key);
			reverseMap.put(String.valueOf(value), String.valueOf(key));
		}

		return reverseMap;
	}
	
	public char getShiftChar(){return shift.char_char;}
	public char getAltChar(){return alt.char_char;}
	public char getCtrlChar(){return control.char_char;}
	public char getEnterChar(){return enter.char_char;}
	public char getLeftParenChar(){return paren_left.char_char;}
	public char getRightParenChar(){return paren_right.char_char;}
	public char getLeftBraceChar(){return brace_left.char_char;}
	public char getRightBraceChar(){return brace_right.char_char;}
	
	public String getSHIFT_END_DELETE(){ return SHIFT_END_DELETE;}
	
	protected void createCommonStrings(){
		//create SHIFT_END_DELETE
		SHIFT_END_DELETE = String.valueOf(getShiftChar()) +
		                   String.valueOf(getLeftBraceChar()) +
		                   END + 
		                   String.valueOf(getRightBraceChar()) +			   	
		                   String.valueOf(getLeftBraceChar()) +
		                   DEL + 
		                   String.valueOf(getRightBraceChar());			   			
	}
	
	protected void addEvents(Vector<RobotKeyEvent> keys, String keychar){
		String keyvalue;
		int _code = 0;
		Object value;
		String tmpChar = keychar;
		if (tmpChar.equals(SPACE)){
			tmpChar = QUOTE+ keychar +QUOTE;
		}
		
		value = standards.get(tmpChar);
		
		if (value == null){
			//to do with 'non-standard'(NLS) string 
			Log.d("InputKeysParser", "InputKeysParser: No matching keycode for characters: '"+ keychar +"'");
			Log.d("InputKeysParser", "...............: Treat it as 'non-standard' string, adding a RobotClipboardPasteEvent event.");
			keys.add(new RobotClipboardPasteEvent(keychar, createPasteEvent()));
		}
		else if (value instanceof Integer){
			_code = ((Integer) value).intValue();
			keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, _code));
		}
		else if (value instanceof String){
			keyvalue = (String)value;
			if (keyvalue.startsWith(SHIFT_PLUS)){
				try{
					_code = Integer.parseInt(keyvalue.substring(6));
					if(! shift_on){
						keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, shift.char_code));
					}
					
					keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, _code));
					
					if(! shift_on){
						keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, shift.char_code));
					}				
				}
				catch(NumberFormatException nfe){
					Log.d("InputKeysParser", "InputKeysParser: Unknown keycode for character: "+ 
					keychar +" : "+ keyvalue);
				}
				catch(IndexOutOfBoundsException ioob){
					Log.d("InputKeysParser", "InputKeysParser: Invalid keycode for character: "+ 
					keychar +" : "+ keyvalue);
				}
			}else{
				Log.d("InputKeysParser", "InputKeysParser: Unsupported modifier for character: "+ keychar +
				          " : "+ keyvalue);
			}
		}else{
			Log.d("InputKeysParser", "InputKeysParser: Unexpected storage type for character: "+ 
			          keychar +" : "+ value.getClass().getName());
		}
	}
	
	/**
	 * <b>Note:</b>     This method do the opposite work of {@link #parseInput(String)}<br>
	 * 
	 * @param keys		List of RobotKeyEvent
	 * @return			String: the script string that user use to input<br>
	 *                  The sring's format should be consistent with specification in 
	 *                  {@link org.safs.tools.input.CreateUnicodeMap}<br><br>
	 * 
	 * Ex.<br>
	 * keys = {
	 * 			RobotKeyEvent(KEY_PRESS,KeyEvent.VK_SHIFT),
	 * 			RobotKeyEvent(KEY_PRESS,KeyEvent.VK_A),
	 * 			RobotKeyEvent(KEY_PRESS,KeyEvent.VK_A),
	 * 			RobotKeyEvent(KEY_TYPE,KeyEvent.VK_A),
	 * 			RobotKeyEvent(KEY_TYPE,KeyEvent.VK_B),
	 * 			RobotKeyEvent(KEY_TYPE,KeyEvent.VK_5),
	 * 			RobotKeyEvent(KEY_TYPE,KeyEvent.VK_D),
	 * 			RobotKeyEvent(KEY_RELEASE,KeyEvent.VK_SHIFT)
	 * 	      }<br><br>
	 * 
	 * Returned string will be AAAB{%}D
 	 */
	public String antiParse(List<RobotKeyEvent> keys){
		StringBuffer script = new StringBuffer();
		RobotKeyEvent event = null;
		int eventType = -1;
		int vkcode = -1;
		boolean shiftOn = false;
		boolean controlOn = false;
		boolean altOn = false;
		
		ListIterator<RobotKeyEvent> listIter = keys.listIterator();
		
		while(listIter.hasNext()){
			event = listIter.next();
			eventType = event.get_event();
			vkcode = event.get_keycode();
			
			switch(eventType){
			case RobotKeyEvent.KEY_PRESS:
				try{
					if(KeyEvent.KEYCODE_SHIFT_LEFT==vkcode){
						script.append(""+shift.char_char+paren_left.char_char);//Add +(
						shiftOn = true;
					}else if(KeyEvent.KEYCODE_ALT_LEFT==vkcode){
						script.append(""+alt.char_char);
						altOn = true;
					}else if(KeyEvent.KEYCODE_CTRL_LEFT==vkcode){ // API 10 throws Error
						script.append(""+control.char_char);
						controlOn = true;
					}else{
						parseKeyCode(script,vkcode,shiftOn);
					}
				}catch(Throwable api10_and_below){
					script.append(""+control.char_char);
					controlOn = true;
				}
				break;
			case RobotKeyEvent.KEY_RELEASE:
				try{
					if(KeyEvent.KEYCODE_SHIFT_LEFT==vkcode){
						script.append(""+paren_right.char_char);//Add )
						shiftOn = false;
					}else if(KeyEvent.KEYCODE_ALT_LEFT==vkcode){
						altOn = false;
					}else if(KeyEvent.KEYCODE_CTRL_LEFT==vkcode){ // API 10 throws error
						controlOn = false;
					}
				}catch(Throwable api10_and_below){
					controlOn = false;
				}
				break;
			case RobotKeyEvent.KEY_TYPE:
				parseKeyCode(script,vkcode,shiftOn);
				break;
			default:
				break;
			}
		}
		
		//We need to remove the void script string "+()" from the StringBuffer
		String original = script.toString();
		String outString = original.replaceAll("\\+\\(\\)", "");
		
		Log.d("InputKeysParser", "InputKeysParser: original is "+original+"   ;  output is  "+outString);
		
		return outString;
	}
	
	/**
	 * <b>Purpose:</b>	    Translate the virtual key code to appropriate charString and append it to the buffer<br>
	 * 
	 * @param script		StringBuffer	Buffer which contains the script string
	 * @param vkcode		int				The virtual key code
	 * @param shiftOn		boolean			If the shift key is pressed
	 * 
	 * @return	void.       The parameter "script" will contain the result string
	 */
	private void parseKeyCode(StringBuffer script, int vkcode, boolean shiftOn){
		String tempChar = null;
		
		//For those special chars INDEPENDENT with the ShiftKey, this means that the vkcode is same
		//whether the ShiftKey is pressed or not. 
		//These special chars, you can find in the section [SPECIAL] of file SAFSKeycodeMap.dat
		//For example like following:
		//		ALT=18
		//		ENTER=10
		//		SHIFT=16
		
		//Firstly, try to get the char from the map specialsKeyToChar by vkcode
		Log.d("InputKeysParser", "InputKeysParser: Try to find the char for virtual key code : "+vkcode+" in the special map.");
		tempChar = specialsKeyToChar.get(String.valueOf(vkcode));
		if(tempChar!=null){
			//If the ShiftKey is pressed, the inputKeyString should be +{Special}, NOT +(XXXX{Special}XXXX)
			//we should transform +(XXXX{Special}XXXX) to +(XXXX)+{Special}+(XXXX)
			//For example: if we press Shift+HOME, the inputString should be +{HOME}, NOT +({HOME})
			if(shiftOn){
				//Add )+{SpecialChar}+(
				script.append(""+paren_right.char_char+shift.char_char+brace_left.char_char+
						         tempChar+
						         brace_right.char_char+shift.char_char+paren_left.char_char);
			}else{
				script.append(brace_left.char_char+tempChar+brace_right.char_char);
			}
		}else{
			//Not all items in the section [SPECIAL] of file SAFSKeycodeMap.dat are INDEPENDENT with the ShiftKey
			//For those values are "SHIFT+vkcode", they are dependent with ShiftKey
			//		^=SHIFT+54
			//		%=SHIFT+53
			Log.d("InputKeysParser", "InputKeysParser: Can NOT find the corresponding char in the special map for virtual key code : "+vkcode);
			if(shiftOn){
				//For the some special chars, if shift is on, we should check the special map firstly.
				//That is, we try to find the "SHIFT+vkcode" in the special map specialsKeyToChar
				//If we get a special char, we should wrap it with brace as {specialChar}, and append it to script string
	//			^=SHIFT+54
	//			%=SHIFT+53

				String charString = SHIFT_PLUS+vkcode;
				Log.d("InputKeysParser", "InputKeysParser: Shift key is pressed, We try to find the char for virtual key code with shitf: "+charString+" in the special map");
				tempChar = specialsKeyToChar.get(String.valueOf(charString));
				if(tempChar!=null){
					//"){SepcialChar}+("
					script.append(""+paren_right.char_char+brace_left.char_char+
							         tempChar+
							         brace_right.char_char+shift.char_char+paren_left.char_char);
				}else{
					Log.d("InputKeysParser", "InputKeysParser: Can NOT find the corresponding char for virtual code with shift : "+charString+" in special map");
					Log.d("InputKeysParser", "InputKeysParser: Try to find the char for virtual key code with shift : "+charString+" in the standard map.");
					//For the some standard upper case chars, if shift is on, we should check the standard map with "SHIFT+vkcode" as key
					//That is, we try to find the "SHIFT+vkcode" in the standard map standardsKeyToChar
					//If we get this upper case char, we will append it directly to the script string
	//				A=SHIFT+65
	//				B=SHIFT+66
	//				C=SHIFT+67
	
	//				!=SHIFT+49
	//				@=SHIFT+50
	//				#=SHIFT+51
					tempChar = standardsKeyToChar.get(String.valueOf(charString));
					if(tempChar!=null){
						//")NormalChar+("
						script.append(""+paren_right.char_char+tempChar+shift.char_char+paren_left.char_char);
					}else{
						//Only the "vkcode=32" will be handled in this branch for now, which is " "
						Log.d("InputKeysParser", "InputKeysParser: Can NOT find the corresponding char for virtual key code with shift : "+charString+" in standard map");
						Log.d("InputKeysParser", "InputKeysParser: Try to find the char for key : "+vkcode+" in the standard map.");
						tempChar = standardsKeyToChar.get(String.valueOf(vkcode));
						if(tempChar!=null){
							//if tempChar is " ", we should change it to a blank without quote
							if(tempChar.equals(QUOTE+ SPACE +QUOTE)) tempChar = SPACE;
							script.append(tempChar);
						}else{
							Log.d("InputKeysParser", "InputKeysParser: Can NOT find the corresponding char for virtual code: "+vkcode+" in standard map.");
						}
					}
				}
			}else{
				Log.d("InputKeysParser", "InputKeysParser: We try to find the char for virtual key code: "+vkcode+" in the standard map.");
				//Try to get the char from the map standardsKeyToChar
				//				a=65
				//				b=66
				//				c=67
				//				1=49
				//				2=50
				//				3=51
				tempChar = standardsKeyToChar.get(String.valueOf(vkcode));
				if(tempChar!=null){
					//if tempChar is " ", we should change it to a blank without quote
					if(tempChar.equals(QUOTE+ SPACE +QUOTE)) tempChar = SPACE;
					script.append(tempChar);
				}else{
					Log.d("InputKeysParser", "InputKeysParser: Can NOT find the corresponding char for virtual code: "+vkcode+" in standard map.");
				}
			}
		}
	}
	
	
	public Vector<RobotKeyEvent> parseInput(String input){
		Vector<RobotKeyEvent> keys = new Vector<RobotKeyEvent>();
		int startgroup = -1;
		int endgroup = -1;
		int cursor = 0;
		char _char;		
		String keychar;
		
		String nonStandardStr;
		
		//loop through the string one char at a time;one sub-string at a time for NLS(non-standard) characters 
		while(cursor < input.length()){		
			_char = input.charAt(cursor);

			// look for key modifier
			if (_char == alt.char_char){
				keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, alt.char_code));
				alt_on = true;

			// look for key modifiers
			}else if (_char == control.char_char){
				keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, control.char_code));
				ctrl_on = true;

			// look for key modifiers
			}else if (_char == shift.char_char){
				keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, shift.char_code));
				shift_on = true;

			}else if (_char == enter.char_char){
				keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, enter.char_code));
				clearModifiers(keys);
				
			}else if (_char == brace_right.char_char){
				keys.addAll(parseBraces(String.valueOf(brace_right.char_char)));
				clearModifiers(keys);
				
			}else if (_char == paren_right.char_char){
				keys.addAll(parseBraces(String.valueOf(paren_right.char_char)));
				clearModifiers(keys);
				
			// process any brace grouping
			}else if(_char == brace_left.char_char){
				startgroup = cursor;
				endgroup = input.indexOf(brace_right.char_char, startgroup);
				if (endgroup > startgroup){
					// check for {}}
					if (endgroup == startgroup+1){
						try{
							if(input.charAt(endgroup+1) == brace_right.char_char){
								Log.d("InputKeysParser", "InputKeyParser adding Escaped RIGHT BRACE.");
								endgroup++;
								//keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, getKeyCode(String.valueOf(brace_right.char_char))));
								keys.addAll(parseBraces(input.substring(startgroup+1, endgroup)));
							}else{							
								Log.d("InputKeysParser", "InputKeyParser does not handle EMPTY BRACES!");
								// (invalid?) empty braces
							}							
						}catch(IndexOutOfBoundsException ib){
							//end of string -- (invalid?) empty braces
						}						
					}else{
						keys.addAll(parseBraces(input.substring(startgroup+1, endgroup)));
					}					
					cursor = endgroup;					
					clearModifiers(keys);
				}else{
					//bad format -- no end brace.
					//use single char as is?
					keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, getKeyCode(input.substring(cursor, cursor+1))));				}
					clearModifiers(keys);
			
			// process any parens
			}else if(_char == paren_left.char_char){
				startgroup = cursor;
				endgroup = input.indexOf(paren_right.char_char, startgroup);
				if (endgroup > startgroup){
					if(endgroup > startgroup+1){					
						keys.addAll(parseParens(input.substring(startgroup+1, endgroup)));
					}else{
						// (invalid?) empty parens
					}
					cursor = endgroup;					
				}else{
					//bad format -- no end paren.
					//use single char as is?
					keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, getKeyCode(input.substring(cursor, cursor+1))));
				}
				clearModifiers(keys);
			
			//process non-standard (NLS) chars			
			}else if((nonStandardStr = getNonStdChars(input,cursor)) != null){
				addEvents(keys, nonStandardStr);
				cursor += nonStandardStr.length();	
				continue;
			//process standard chars 
			}else{
				keychar=input.substring(cursor, cursor+1);
				addEvents(keys, keychar);
				clearModifiers(keys);	
			}
			cursor++;		
		} 
		return keys;
	}
	
	public Vector<RobotKeyEvent> parseChars(String input){
		Vector<RobotKeyEvent> keys = new Vector<RobotKeyEvent>();
		int cursor = 0;
		String keychar;
				
		//loop through the string one char at a time for standard char;one sub-string at a time for NLS(non-standard) charactars 
		while(cursor < input.length()){		
			String nonStandardStr = getNonStdChars(input,cursor);
			if(nonStandardStr != null){
				addEvents(keys, nonStandardStr);
				cursor += nonStandardStr.length();
			}else{
				keychar =  input.substring(cursor, cursor+1);
				addEvents(keys, keychar);
				cursor++;		
			}
		} 
		return keys;
	}

	
	/**
	 * Retrieve the stored "standard" keycode of the provided character.  
	 * @param _char -- String of one char.
	 * @return Integer of keycode OR'd with key modifiers. -1 OR'd with modifiers 
	 * if the char keycode is not found.
	 */	
	protected int getKeyCode(String _char){
		// some keycodes are going to be "SHIFT+code" ???
		Object value = null;
		try{ 
			//return ((Integer) standards.get( _char )).intValue();
			value = standards.get(_char);
			if( value instanceof Integer){
				return ((Integer)value).intValue();
			}else{ // likely instanceof String
				if (value == null) throw new NullPointerException("No keycode for "+ _char);
				Log.d("InputKeysParser", "IKP: Not handling getKeyCode '"+ value.toString()+ "' for _char: "+_char);
			}
		}
		catch(NullPointerException npe){
			Log.d("InputKeysParser", "IKP: Ignoring getKeyCode NullPointerException for: "+_char, npe);
		}
		return -1;		
	}
	
	/** 
	 * Convert literal string characters into their corresponding keycodes.
	 * This is normally a number of characters to which a modifier is applied.
	 * @param content -- the string of characters to convert to keycodes.
	 * @return Vector of Integer keycodes
	 */
	protected Vector<RobotKeyEvent> parseString(String content){
		Vector<RobotKeyEvent> keys = new Vector<RobotKeyEvent>();
		String keychar;
		try{
			int len = content.length();
			for(int i=0;i<len;i++){
				keychar = content.substring(i,i+1);
				addEvents(keys, keychar);
			}
		}catch(NullPointerException npe){ ; }
		return keys;
	}
	
	/** 
	 * Process the string previously extracted from between parens.
	 * This is normally a number of characters to which a modifier is applied.
	 * @param content -- the string in between parens ( )
	 * @return Vector of Integer keycodes
	 */
	protected Vector<RobotKeyEvent> parseParens(String content){
		
		return parseString(content);
	}
	
	/** 
	 * Process the string previously extracted from between braces.
	 * This is one special character String or a standard character followed by a 
	 * space and the number of times to repeat it. 
	 * @param content -- the string in between braces { }
	 * @return Vector of Integer keycodes
	 */
	protected Vector<RobotKeyEvent> parseBraces(String content){
		Vector<RobotKeyEvent> keys = new Vector<RobotKeyEvent>();
		int sep;
		int repeat = 1;
		int icode = -1;
		String key = null;
		Object value = null;
		RobotKeyEvent event = null;
		
		key = content.trim();
		try{
			//check for space separator
			sep = content.indexOf(SPACE);
			//if found, process repeat count
			if (sep > 0){
				key = key.substring(0, sep);
				try{
					repeat = Integer.parseInt(content.substring(sep+1).trim());
					if (repeat < 1) repeat = 1; 
				}catch(IndexOutOfBoundsException ioob){
					// what to do?  Report illegal format?
					Log.d("InputKeysParser", "IKP: Ignoring bad or missing InputKeys REPEAT format.");
				}catch(NumberFormatException nfe){
					// what to do?  Report illegal format?
					Log.d("InputKeysParser", "IKP: Ignoring bad InputKeys REPEAT format. ");
				}
			}
			//add key repeat 
			try{
				value = specials.get(key);
				if (value == null) value = standards.get(key);
				if(value instanceof Integer){
					icode = ((Integer)value).intValue();
					event = new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, icode);
					for(int i = 0; i < repeat;i++){
						keys.add(event);
					}					
				}
				else if (value instanceof String){
					String keyvalue = (String)value;
					if (keyvalue.startsWith(SHIFT_PLUS)){
						try{
							icode = Integer.parseInt(keyvalue.substring(6));
							if(! shift_on){
								keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, shift.char_code));
							}
							for(int i = 0; i < repeat; i++){
								keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_TYPE, icode));
							}
							if(! shift_on){
								keys.add(new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, shift.char_code));
							}
						}
						catch(NumberFormatException nfe){
							Log.d("InputKeysParser", "InputKeysParser: Unknown keycode for special character: "+ 
							key +" : "+ keyvalue);
						}
						catch(IndexOutOfBoundsException ioob){
							Log.d("InputKeysParser", "InputKeysParser: Invalid keycode for special character: "+ 
							key +" : "+ keyvalue);
						}
					}else{
						Log.d("InputKeysParser", "InputKeysParser: Unsupported modifier for special character: "+ 
								key +" : "+ keyvalue);
					}
				}else{
					if (value == null){
						Log.d("InputKeysParser", "InputKeysParser: Unexpected NULL for special character: "+ 
						          key +".");
					}else{
						Log.d("InputKeysParser", "InputKeysParser: Unexpected storage type for special character: "+ 
					          key +" : "+ value.getClass().getName());
					}
				}
			}catch(NullPointerException np2){
				Log.d("InputKeysParser", "IKP: Ignoring unknown InputKeys string. ", np2);
			}catch(ClassCastException cce){
				Log.d("InputKeysParser", "IKP: Ignoring unknown InputKeys format. ", cce);
			}
			
		}catch(NullPointerException npe){ ;	}
		return keys;
	}

	private void setStringInfo(INIFileReader config, String section, Hashtable store){
		Vector _list = config.getItems(section);
		try{
			String item;
			String value;
			Iterator it = _list.iterator();
			while(it.hasNext()){
				item = (String) it.next();
				value = config.getAppMapItem(section, item);
				try{ 
					store.put(item, new Integer(value));
				}
				catch(NumberFormatException nfe){ 
					store.put(item, value );
				}
			}			
		}catch(NullPointerException np){}
	}

	private void setTokenInfo(INIFileReader config, CharInfo info, String token){
		String value = config.getAppMapItem(CreateUnicodeMap.TOKENS, token);		
		if ((!(value == null))&&(value.length() > 0)) {
			info.char_char = value.charAt(0);
			try{info.char_code = Integer.parseInt(config.getAppMapItem(CreateUnicodeMap.TOKENS, value));}
			catch(NumberFormatException nfx){ info.char_code = -1; }
		} 
	}

	/**
	 * Add keyRelease events to the keystroke Vector and clear boolean flags for active modifiers.
	 * @param keys Vector to add keyRelease events to.
	 */
	protected void clearModifiers(Vector<RobotKeyEvent> keys){
		if(alt_on){
			alt_on = false;
			RobotKeyEvent event = new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, alt.char_code);
			keys.add(event);
		}
		if(ctrl_on){
			ctrl_on = false;
			RobotKeyEvent event = new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, control.char_code);
			keys.add(event);
		}
		if(shift_on){
			shift_on = false;
			RobotKeyEvent event = new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, shift.char_code);
			keys.add(event);
		}
	}

	/** Check input string from the element at postion 'fromIdx' to get a "non-standard" sub-string.
	 *  "non-standard" string means that every _char inside it has no mapping found in the stored "standard" 
	 *  keycode file, "SAFSKeyCodeMap.dat". 
	 * @param input,   a input string.
	 * @param fromIdx, 0-based index started from.
	 * @return: a "non-stardard" sub-string starting fromIdx; null, not found.
	 * @see SAFSKeyCodeMap.dat
	 */
	private String getNonStdChars(String input, int fromIdx){
		if(input == null) 
			return null;
		int len = input.length();
		if(fromIdx < 0 || fromIdx>=len)
			return null;
		int endPos;
		for(endPos=fromIdx; endPos<len; endPos++){
			String tmpkeychar =  input.substring(endPos, endPos+1);
			if(standards.get(tmpkeychar) != null)
				break;
		}	
		if(endPos == fromIdx)
			return null;
		else
			return input.substring(fromIdx,endPos);
	}
	/* create hot keys (Ctrl+v) to paste from system Clipboard */
	protected Vector<RobotKeyEvent> createPasteEvent(){
		Vector<RobotKeyEvent> pasteEvents = new Vector<RobotKeyEvent>(3);		
		clearModifiers(pasteEvents);
		//TODO Menu+v to copy string from clipboard
		pasteEvents.add(new RobotKeyEvent(RobotKeyEvent.KEY_PRESS, KeyEvent.KEYCODE_MENU));
		addEvents(pasteEvents,"v");		
		pasteEvents.add(new RobotKeyEvent(RobotKeyEvent.KEY_RELEASE, KeyEvent.KEYCODE_MENU));
		return pasteEvents;
	}
	
	public class CharInfo {
		public char char_char = ' ';
		public int char_code = -1;
		public CharInfo(char _char, int code){
			char_char = _char;
			char_code = code;
		}
	}

	/** simple test\debugging only */
	public static void main(String[] args) {
		INIFileReader reader = new INIFileReader(ClassLoader.getSystemResourceAsStream(CreateUnicodeMap.DEFAULT_FILE + CreateUnicodeMap.DEFAULT_FILE_EXT), 0, false);
		InputKeysParser parser = new InputKeysParser(reader);
		Vector<RobotKeyEvent> input = parser.parseInput("a");
		input = parser.parseInput("A");
		input = parser.parseInput("0");
		input = parser.parseInput("^");		
		input = parser.parseInput("{");
		input = parser.parseInput("}");
		input = parser.parseInput("(");
		input = parser.parseInput(")");
		input = parser.parseInput("%");		
	}	
}
