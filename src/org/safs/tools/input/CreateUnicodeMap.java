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


import android.view.KeyEvent;
import java.io.*;

/**
 * Create a Unicode output file for the running version of Java that can be used 
 * during testing and test development to map SAFS keystroke definitions to the 
 * Java keycodes needed by java.awt.Robot.
 * <p>
 * This class can be used to map the default US English 
 * character map and then may be copied and\or modified to generate keycode 
 * character maps for other locales or languages.  The generation of the US 
 * English map is already done and provided with the released package.
 * <p>
 * The class creates a file (SAFSKeycodeMap.dat) mapping text string sequences to the keycode 
 * used by the AWT Robot.  For example:
 * <p><ul>
 * ENTER=10
 * </ul>
 * <p> 
 * The example shows that the literal text "ENTER" will be mapped to keycode integer 
 * 10.  These keycodes are generally those defined in the java.awt.event.KeyEvent 
 * class.
 * <p>
 * SAFS text string sequences for commands like InputKeys are supported as follows.
 * In general, these are the same strings used by IBM Rational Robot and Microsoft 
 * standards for defining keystrokes in scripting languages:
 * <p><ul>
 * Some characters in the string are passed to the active window as literal characters, 
 * meaning that they are passed just as they appear in the string � for example, the 
 * letters a through z and the numbers 0 through 9. 
 * <p>
 * The following characters cause the associated keystroke to be performed:
 * <p><ul><pre>
 * ~	Causes the Enter key to be pressed.
 * +	Causes the Shift key to be pressed and held down while the next character 
 *      is pressed.
 * ^	Causes the Control key to be pressed and held down while the next character 
 *      is pressed.
 * %	Causes the Alt key to be pressed and held down while the next character is pressed.
 * </pre></ul>
 * <p>
 * If a group of characters is enclosed in parentheses, all the characters are affected 
 * by the special character that precedes the parentheses. For example, the following 
 * string inserts ABCD into the active window:
 * <p><ul>
 * "+(abcd)"
 * <p></ul>
 * Keys associated with non-printable characters (such as the Escape key and arrow keys) 
 * and keys on the numeric and extended keypads are represented by descriptive names in 
 * curly braces ( {} ). Names are not case-sensitive. The valid key names you can specify 
 * in curly braces are included in the table at the end of the Comments section.
 * <p>
 * To insert one of the above special characters � that is, ~+^%({ � as itself rather 
 * than as the special activity that it represents, enclose the character in curly 
 * braces. For example, the following command inserts a plus sign (+) into the active 
 * window:
 * <p><ul>
 * "{+}"
 * <p></ul>
 * Use the following table to determine the Keytext$ for the keyboard key you want:
 * <p><ul>
 * <table>
 * <tr>
 * <td>Keytext value	<td>Keyboard equivalent 
 * <tr><td>Actual printable character.<br>Examples:  A1.&
 * <td>Letters A�Z, a�z, numbers 0�9, punctuation, other printable characters on the main 
 * keyboard.
 * <tr><td>{Alt}<td>Default Alt key (either left or right).
 * <tr><td>{BackSpace}<br>{BS}<br>{BkSp}<td>	Backspace.
 * <tr><td>{Break}<td>Break or Pause.
 * <tr><td>{CapsLock}<td>Caps Lock.
 * <tr><td>{Clear}<td>Clear.
 * <tr><td>{Ctrl}<td>Default Control key (either left or right).
 * <tr><td>{Delete} or {Del} or {NumDelete} or {ExtDelete}<td>Delete.
 * <tr><td>{Down} or {NumDown} or {ExtDown}<td>Down Arrow.
 * <tr><td>{End} or {NumEnd} or {ExtEnd}<td>End.
 * <tr><td>{Enter} or ~ or {NumEnter} or {Num~}<td>Enter.
 * <tr><td>{Escape} or {Esc}<td>Escape.
 * <tr><td>{Help}<td>Help.
 * <tr><td>{Home} or {NumHome} or {ExtHome}<td>Home.
 * <tr><td>{Insert} or {NumInsert} or {ExtInsert}<td>Insert.
 * <tr><td>{Left} or {NumLeft} or {ExtLeft}<td>Left Arrow.
 * <tr><td>{NumLock}<td>Num Lock.
 * <tr><td>{PgDn} or {NumPgDn} or {ExtPgDn}<td>Page Down.
 * <tr><td>{PgUp} or {NumPgUp} or {ExtPgUp}<td>Page Up.
 * <tr><td>{PrtSc}<td>Print Screen.
 * <tr><td>{Right} or {NumRight} or {ExtRight}<td>Right Arrow.
 * <tr><td>{ScrollLock}<td>Scroll Lock.
 * <tr><td>{Shift}<td>Default Shift key (either left or right).
 * <tr><td>{Tab}<td>Tab.
 * <tr><td>{Up} or {NumUp} or {ExtUp}<td>Up Arrow.
 * <tr><td>{Numn}, where n is a number from 0 through 9 Example:  {Num5}<td>0-9 (numeric keypad).
 * <tr><td>{Num.} or .<td>. (period, decimal).
 *  <tr><td>{Num-} or -<td>- (dash, subtraction sign).
 * <tr><td>{Num*} or *<td>* (asterisk, multiplication sign).
 * <tr><td>{Num/} or /<td>/ (slash, division sign).
 * <tr><td>{Num+} or {+}<td>+ (addition sign).
 * <tr><td>{^}<td>^ (caret character).
 * <tr><td>{%}<td>% (percent character).
 * <tr><td>{~}<td>~ (tilde character).
 * <tr><td>{(}<td>( (left parenthesis character).
 * <tr><td>) or {)}<td>) (right parenthesis character).
 * <tr><td>{{}<td>{ (left brace character).
 * <tr><td>} or {}}<td>} (right brace character).
 * <tr><td>[<td>[ (left bracket character).
 * <tr><td>]<td>] (right bracket character).
 * <tr><td>{F#}<br>Example:  {F6}<td>F# (function keys 1-12).
 * <tr><td>+<br>Example:  +{F6}<td>Shift (used while pressing down another key).
 * <tr><td>^<br>Example:  ^{F6}<td>Control (used while pressing down another key).
 * <tr><td>%<br>Example:  %{F6}<td>Alt (used while pressing down another key).
 * <tr><td>{key n},<br>where key is any key, and n is the number of times that key is pressed.<br>
 * Example:  {a 10}<td>Repeats the key press n number of times.
 * </table></ul>
 * </ul>
 * 
 * @author CarlNagle FEB 13, 2007
 * @see java.awt.Robot
 * @see java.awt.event.KeyEvent
 * @see org.safs.staf.service.keys.InputKeysParser
 * @see org.safs.tools.input.RobotKeyEvent
 */
public class CreateUnicodeMap {

	/** safs_keycode_map */
	public static final String DEFAULT_FILE="safs_keycode_map";
	/** .dat */
	public static final String DEFAULT_FILE_EXT=".dat";
	
	public static final String TOKENS = "TOKENS";
	public static final String STANDARD = "STANDARD";
	public static final String SPECIAL = "SPECIAL";

	//Did not find brace keycode in android, we will use bracket instead
	//TODO Brace keycode doesn't exist in Android
	public static final String BRACELEFT = "BRACELEFT";
	public static final String BRACERIGHT = "BRACERIGHT";
	public static final String PARENLEFT = "PARENLEFT";
	public static final String PARENRIGHT = "PARENRIGHT";
	public static final String ALT = "ALT";
	public static final String CONTROL = "CONTROL";
	public static final String SHIFT = "SHIFT";
	public static final String ENTER = "ENTER";
	
	
	private static String newLine = System.getProperty("line.separator");
	
	/**
	 * Return the current line separator used for file output.  By default 
	 * this is the System line separator.
	 * 
	 * @return  the current line separator used for file output.
	 */
	public static String getNewLine(){ return newLine;}
	/**
	 * Set a different file line separator.  By default the System default 
	 * line separator is used.
	 * 
	 * @param lineseparator -- the String to use for line separation in the 
	 * output file.
	 */
	public static void setNewLine(String lineseparator){
		newLine = lineseparator;
	}	
	
	public static void addEntry(BufferedWriter outfile, String mapstring, int keycode)
	                     throws IOException{
		outfile.write(mapstring +"="+ String.valueOf(keycode));
		outfile.write(newLine);
	}

	public static void addEntry(BufferedWriter outfile, String mapstring, String keycode)
						 throws IOException{
		outfile.write(mapstring +"="+ keycode);
		outfile.write(newLine);
	}
    
	public static void main(String[] args) {
		output1(args);
		//output2(args);//debugging only 
	}
	
	public static void output1(String[] args) {
		//open Unicode file for output
		try{
			BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter
			                            (new FileOutputStream
			                            (new File(DEFAULT_FILE + DEFAULT_FILE_EXT)
			                             ), "UTF-8"
			                             ));
			                             
			//parsing tokens
			outfile.write(newLine);
			outfile.write("["+ TOKENS +"]");
			outfile.write(newLine);
			
			outfile.write(BRACELEFT +"={"+ newLine);
			outfile.write(BRACERIGHT +"=}"+ newLine);
			outfile.write(PARENLEFT +"=("+ newLine);
			outfile.write(PARENRIGHT +"=)"+ newLine);

			outfile.write(ALT +"=%"+ newLine);
			outfile.write(CONTROL +"=^"+ newLine);
			outfile.write(SHIFT +"=+"+ newLine);
			outfile.write(ENTER +"=~"+ newLine);

			outfile.write(newLine);

			addEntry(outfile, "%", KeyEvent.KEYCODE_ALT_LEFT);
			addEntry(outfile, "+", KeyEvent.KEYCODE_SHIFT_LEFT);
			addEntry(outfile, "~", KeyEvent.KEYCODE_ENTER);
			
			//TODO There is no keycode for brace and parentheses
//			addEntry(outfile, "{", KeyEvent.KEYCODE_LEFT_BRACKET);
//			addEntry(outfile, "}", KeyEvent.KEYCODE_RIGHT_BRACKET);
			addEntry(outfile, "{", "SHIFT+"+ KeyEvent.KEYCODE_LEFT_BRACKET);
			addEntry(outfile, "}", "SHIFT+"+ KeyEvent.KEYCODE_RIGHT_BRACKET);
//			addEntry(outfile, "(", KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);//Need KEYCODE_NUM_LOCK pressed?, some device need, some not!!!!
//			addEntry(outfile, ")", KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);//Need KEYCODE_NUM_LOCK pressed?
			addEntry(outfile, "(", "SHIFT+"+ KeyEvent.KEYCODE_9);
			addEntry(outfile, ")", "SHIFT+"+ KeyEvent.KEYCODE_0);
			
			//standard keys
			outfile.write(newLine);
			outfile.write("["+ STANDARD +"]");
			outfile.write(newLine);
			
		    addEntry(outfile, "0", KeyEvent.KEYCODE_0);
			addEntry(outfile, "1", KeyEvent.KEYCODE_1);
			addEntry(outfile, "2", KeyEvent.KEYCODE_2);
			addEntry(outfile, "3", KeyEvent.KEYCODE_3);
			addEntry(outfile, "4", KeyEvent.KEYCODE_4);
			addEntry(outfile, "5", KeyEvent.KEYCODE_5);
			addEntry(outfile, "6", KeyEvent.KEYCODE_6);
			addEntry(outfile, "7", KeyEvent.KEYCODE_7);
			addEntry(outfile, "8", KeyEvent.KEYCODE_8);
			addEntry(outfile, "9", KeyEvent.KEYCODE_9);
			
			outfile.write(newLine);

			addEntry(outfile, "a", KeyEvent.KEYCODE_A);
			addEntry(outfile, "b", KeyEvent.KEYCODE_B);
			addEntry(outfile, "c", KeyEvent.KEYCODE_C);
			addEntry(outfile, "d", KeyEvent.KEYCODE_D);
			addEntry(outfile, "e", KeyEvent.KEYCODE_E);
			addEntry(outfile, "f", KeyEvent.KEYCODE_F);
			addEntry(outfile, "g", KeyEvent.KEYCODE_G);
			addEntry(outfile, "h", KeyEvent.KEYCODE_H);
			addEntry(outfile, "i", KeyEvent.KEYCODE_I);
			addEntry(outfile, "j", KeyEvent.KEYCODE_J);
			addEntry(outfile, "k", KeyEvent.KEYCODE_K);
			addEntry(outfile, "l", KeyEvent.KEYCODE_L);
			addEntry(outfile, "m", KeyEvent.KEYCODE_M);
			addEntry(outfile, "n", KeyEvent.KEYCODE_N);
			addEntry(outfile, "o", KeyEvent.KEYCODE_O);
			addEntry(outfile, "p", KeyEvent.KEYCODE_P);
			addEntry(outfile, "q", KeyEvent.KEYCODE_Q);
			addEntry(outfile, "r", KeyEvent.KEYCODE_R);
			addEntry(outfile, "s", KeyEvent.KEYCODE_S);
			addEntry(outfile, "t", KeyEvent.KEYCODE_T);
			addEntry(outfile, "u", KeyEvent.KEYCODE_U);
			addEntry(outfile, "v", KeyEvent.KEYCODE_V);
			addEntry(outfile, "w", KeyEvent.KEYCODE_W);
			addEntry(outfile, "x", KeyEvent.KEYCODE_X);
			addEntry(outfile, "y", KeyEvent.KEYCODE_Y);
			addEntry(outfile, "z", KeyEvent.KEYCODE_Z);

			outfile.write(newLine);

			addEntry(outfile, "A", "SHIFT+"+ KeyEvent.KEYCODE_A);
			addEntry(outfile, "B", "SHIFT+"+ KeyEvent.KEYCODE_B);
			addEntry(outfile, "C", "SHIFT+"+ KeyEvent.KEYCODE_C);
			addEntry(outfile, "D", "SHIFT+"+ KeyEvent.KEYCODE_D);
			addEntry(outfile, "E", "SHIFT+"+ KeyEvent.KEYCODE_E);
			addEntry(outfile, "F", "SHIFT+"+ KeyEvent.KEYCODE_F);
			addEntry(outfile, "G", "SHIFT+"+ KeyEvent.KEYCODE_G);
			addEntry(outfile, "H", "SHIFT+"+ KeyEvent.KEYCODE_H);
			addEntry(outfile, "I", "SHIFT+"+ KeyEvent.KEYCODE_I);
			addEntry(outfile, "J", "SHIFT+"+ KeyEvent.KEYCODE_J);
			addEntry(outfile, "K", "SHIFT+"+ KeyEvent.KEYCODE_K);
			addEntry(outfile, "L", "SHIFT+"+ KeyEvent.KEYCODE_L);
			addEntry(outfile, "M", "SHIFT+"+ KeyEvent.KEYCODE_M);
			addEntry(outfile, "N", "SHIFT+"+ KeyEvent.KEYCODE_N);
			addEntry(outfile, "O", "SHIFT+"+ KeyEvent.KEYCODE_O);
			addEntry(outfile, "P", "SHIFT+"+ KeyEvent.KEYCODE_P);
			addEntry(outfile, "Q", "SHIFT+"+ KeyEvent.KEYCODE_Q);
			addEntry(outfile, "R", "SHIFT+"+ KeyEvent.KEYCODE_R);
			addEntry(outfile, "S", "SHIFT+"+ KeyEvent.KEYCODE_S);
			addEntry(outfile, "T", "SHIFT+"+ KeyEvent.KEYCODE_T);
			addEntry(outfile, "U", "SHIFT+"+ KeyEvent.KEYCODE_U);
			addEntry(outfile, "V", "SHIFT+"+ KeyEvent.KEYCODE_V);
			addEntry(outfile, "W", "SHIFT+"+ KeyEvent.KEYCODE_W);
			addEntry(outfile, "X", "SHIFT+"+ KeyEvent.KEYCODE_X);
			addEntry(outfile, "Y", "SHIFT+"+ KeyEvent.KEYCODE_Y);
			addEntry(outfile, "Z", "SHIFT+"+ KeyEvent.KEYCODE_Z);

			outfile.write(newLine);

			addEntry(outfile, "`", KeyEvent.KEYCODE_GRAVE);
			addEntry(outfile, "~", "SHIFT+"+ KeyEvent.KEYCODE_GRAVE);
			addEntry(outfile, "!", "SHIFT+"+ KeyEvent.KEYCODE_1);
			addEntry(outfile, "@", KeyEvent.KEYCODE_AT);
			addEntry(outfile, "#", KeyEvent.KEYCODE_POUND);
			addEntry(outfile, "$", "SHIFT+"+ KeyEvent.KEYCODE_4);
			addEntry(outfile, "%", "SHIFT+"+ KeyEvent.KEYCODE_5);
			addEntry(outfile, "^", "SHIFT+"+ KeyEvent.KEYCODE_6);
			addEntry(outfile, "&", "SHIFT+"+ KeyEvent.KEYCODE_7);
			
			addEntry(outfile, "*", KeyEvent.KEYCODE_STAR);
			//TODO the keycode of numpad sometimes doesn't work on some device
//			addEntry(outfile, "(", KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN);
//			addEntry(outfile, ")", KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN);
			addEntry(outfile, "(", "SHIFT+"+ KeyEvent.KEYCODE_9);//KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN
			addEntry(outfile, ")", "SHIFT+"+ KeyEvent.KEYCODE_0);//KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN

			addEntry(outfile, "-", KeyEvent.KEYCODE_MINUS);
			addEntry(outfile, "_", "SHIFT+"+ KeyEvent.KEYCODE_MINUS);
			addEntry(outfile, "=", KeyEvent.KEYCODE_EQUALS);
			addEntry(outfile, "+", KeyEvent.KEYCODE_PLUS);
			addEntry(outfile, "[", KeyEvent.KEYCODE_LEFT_BRACKET);
			addEntry(outfile, "{", "SHIFT+"+ KeyEvent.KEYCODE_LEFT_BRACKET);
			addEntry(outfile, "]", KeyEvent.KEYCODE_RIGHT_BRACKET);
			addEntry(outfile, "}", "SHIFT+"+ KeyEvent.KEYCODE_RIGHT_BRACKET);			
			addEntry(outfile, "\\", KeyEvent.KEYCODE_BACKSLASH);
			addEntry(outfile, "|", "SHIFT+"+ KeyEvent.KEYCODE_BACKSLASH);
			addEntry(outfile, "'", KeyEvent.KEYCODE_APOSTROPHE);
			addEntry(outfile, "\"", "SHIFT+"+ KeyEvent.KEYCODE_APOSTROPHE);
			addEntry(outfile, ";", KeyEvent.KEYCODE_SEMICOLON);
			addEntry(outfile, ":", "SHIFT+"+ KeyEvent.KEYCODE_SEMICOLON);
			addEntry(outfile, ",", KeyEvent.KEYCODE_COMMA);
			addEntry(outfile, "<", "SHIFT+"+ KeyEvent.KEYCODE_COMMA);
			addEntry(outfile, ".", KeyEvent.KEYCODE_PERIOD);
			addEntry(outfile, ">", "SHIFT+"+ KeyEvent.KEYCODE_PERIOD);
			addEntry(outfile, "/", KeyEvent.KEYCODE_SLASH);
			addEntry(outfile, "?", "SHIFT+"+ KeyEvent.KEYCODE_SLASH);
			addEntry(outfile, "\" \"", KeyEvent.KEYCODE_SPACE);
			
			//special keys (inside braces)
			outfile.write(newLine);
			outfile.write("["+ SPECIAL +"]");
			outfile.write(newLine);

			addEntry(outfile, "ALT", KeyEvent.KEYCODE_ALT_LEFT);
			addEntry(outfile, "ENTER", KeyEvent.KEYCODE_ENTER);
			addEntry(outfile, "SHIFT", KeyEvent.KEYCODE_SHIFT_LEFT);
			addEntry(outfile, "CTRL", KeyEvent.KEYCODE_DPAD_LEFT);
			addEntry(outfile, "BACKSPACE", KeyEvent.KEYCODE_BACK);
			addEntry(outfile, "BS", KeyEvent.KEYCODE_BACK);
			addEntry(outfile, "BKSP", KeyEvent.KEYCODE_BACK);
			addEntry(outfile, "CLEAR", KeyEvent.KEYCODE_CLEAR);
			addEntry(outfile, "DELETE", KeyEvent.KEYCODE_DEL);
			addEntry(outfile, "DEL", KeyEvent.KEYCODE_DEL);
			try{
				addEntry(outfile, "^", KeyEvent.KEYCODE_CTRL_LEFT);
				addEntry(outfile, "BREAK", KeyEvent.KEYCODE_BREAK);
			    addEntry(outfile, "PAUSE", KeyEvent.KEYCODE_BREAK);
				addEntry(outfile, "CAPSLOCK", KeyEvent.KEYCODE_CAPS_LOCK);
				addEntry(outfile, "HOME", KeyEvent.KEYCODE_MOVE_HOME);
				addEntry(outfile, "END", KeyEvent.KEYCODE_MOVE_END);
				addEntry(outfile, "ESCAPE", KeyEvent.KEYCODE_ESCAPE);
				addEntry(outfile, "ESC", KeyEvent.KEYCODE_ESCAPE);
				addEntry(outfile, "F1", KeyEvent.KEYCODE_F1);
				addEntry(outfile, "F2", KeyEvent.KEYCODE_F2);
				addEntry(outfile, "F3", KeyEvent.KEYCODE_F3);
				addEntry(outfile, "F4", KeyEvent.KEYCODE_F4);
				addEntry(outfile, "F5", KeyEvent.KEYCODE_F5);
				addEntry(outfile, "F6", KeyEvent.KEYCODE_F6);
				addEntry(outfile, "F7", KeyEvent.KEYCODE_F7);
				addEntry(outfile, "F8", KeyEvent.KEYCODE_F8);
				addEntry(outfile, "F9", KeyEvent.KEYCODE_F9);
				addEntry(outfile, "F10", KeyEvent.KEYCODE_F10);
				addEntry(outfile, "F11", KeyEvent.KEYCODE_F11);
				addEntry(outfile, "F12", KeyEvent.KEYCODE_F12);
				addEntry(outfile, "INSERT", KeyEvent.KEYCODE_INSERT);
				addEntry(outfile, "PRTSC", KeyEvent.KEYCODE_SYSRQ);
				addEntry(outfile, "SCROLLLOCK", KeyEvent.KEYCODE_SCROLL_LOCK);
				addEntry(outfile, "NUM/", KeyEvent.KEYCODE_NUMPAD_DIVIDE);
				addEntry(outfile, "NUM*", KeyEvent.KEYCODE_NUMPAD_MULTIPLY);
				addEntry(outfile, "NUM-", KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
				addEntry(outfile, "NUM+", KeyEvent.KEYCODE_NUMPAD_ADD);
				addEntry(outfile, "NUM.", KeyEvent.KEYCODE_NUMPAD_DOT);
				addEntry(outfile, "NUM0", KeyEvent.KEYCODE_NUMPAD_0);
				addEntry(outfile, "NUM1", KeyEvent.KEYCODE_NUMPAD_1);
				addEntry(outfile, "NUM2", KeyEvent.KEYCODE_NUMPAD_2);
				addEntry(outfile, "NUM3", KeyEvent.KEYCODE_NUMPAD_3);
				addEntry(outfile, "NUM4", KeyEvent.KEYCODE_NUMPAD_4);
				addEntry(outfile, "NUM5", KeyEvent.KEYCODE_NUMPAD_5);
				addEntry(outfile, "NUM6", KeyEvent.KEYCODE_NUMPAD_6);
				addEntry(outfile, "NUM7", KeyEvent.KEYCODE_NUMPAD_7);
				addEntry(outfile, "NUM8", KeyEvent.KEYCODE_NUMPAD_8);
				addEntry(outfile, "NUM9", KeyEvent.KEYCODE_NUMPAD_9);
				addEntry(outfile, "NUMINSERT", KeyEvent.KEYCODE_INSERT);
				addEntry(outfile, "EXTINSERT", KeyEvent.KEYCODE_INSERT);
				addEntry(outfile, "NUMEND", KeyEvent.KEYCODE_MOVE_END);
				addEntry(outfile, "EXTEND", KeyEvent.KEYCODE_MOVE_END);
				addEntry(outfile, "NUMHOME", KeyEvent.KEYCODE_MOVE_HOME);
				addEntry(outfile, "EXTHOME", KeyEvent.KEYCODE_MOVE_HOME);
				addEntry(outfile, "NUMLOCK", KeyEvent.KEYCODE_NUM_LOCK);
			}
			catch(Throwable x){/* API 11 and up only */}
			//Android doesn't have keycode for following String
//			addEntry(outfile, "F13", KeyEvent.KEYCODE_F13);
//			addEntry(outfile, "F14", KeyEvent.KEYCODE_F14);
//			addEntry(outfile, "F15", KeyEvent.KEYCODE_F15);
//			addEntry(outfile, "F16", KeyEvent.KEYCODE_F16);
//			addEntry(outfile, "F17", KeyEvent.KEYCODE_F17);
//			addEntry(outfile, "F18", KeyEvent.KEYCODE_F18);
//			addEntry(outfile, "F19", KeyEvent.KEYCODE_F19);
//			addEntry(outfile, "F20", KeyEvent.KEYCODE_F20);
//			addEntry(outfile, "F21", KeyEvent.KEYCODE_F21);
//			addEntry(outfile, "F22", KeyEvent.KEYCODE_F22);
//			addEntry(outfile, "F23", KeyEvent.KEYCODE_F23);
//			addEntry(outfile, "F24", KeyEvent.KEYCODE_F24);
//			addEntry(outfile, "HELP", KeyEvent.KEYCODE_HELP);
			addEntry(outfile, "PGDN", KeyEvent.KEYCODE_PAGE_DOWN);
			addEntry(outfile, "PGUP", KeyEvent.KEYCODE_PAGE_UP);
			addEntry(outfile, "TAB", KeyEvent.KEYCODE_TAB);
			
			outfile.write(newLine);


			outfile.write(newLine);

			addEntry(outfile, "LEFT", KeyEvent.KEYCODE_DPAD_LEFT);
			addEntry(outfile, "NUMLEFT", KeyEvent.KEYCODE_DPAD_LEFT);
			addEntry(outfile, "EXTLEFT", KeyEvent.KEYCODE_DPAD_LEFT);
			addEntry(outfile, "RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
			addEntry(outfile, "NUMRIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
			addEntry(outfile, "EXTRIGHT", KeyEvent.KEYCODE_DPAD_RIGHT);
			addEntry(outfile, "UP", KeyEvent.KEYCODE_DPAD_UP);
			addEntry(outfile, "NUMUP", KeyEvent.KEYCODE_DPAD_UP);
			addEntry(outfile, "EXTUP", KeyEvent.KEYCODE_DPAD_UP);
			addEntry(outfile, "DOWN", KeyEvent.KEYCODE_DPAD_DOWN);
			addEntry(outfile, "NUMDOWN", KeyEvent.KEYCODE_DPAD_DOWN);
			addEntry(outfile, "EXTDOWN", KeyEvent.KEYCODE_DPAD_DOWN);

			outfile.write(newLine);

			//TODO the keycode of numpad sometimes doesn't work on some device, use KEYCODE_ENTER instead			
			addEntry(outfile, "NUMENTER", KeyEvent.KEYCODE_ENTER);//KeyEvent.KEYCODE_NUMPAD_ENTER
			addEntry(outfile, "NUM~", KeyEvent.KEYCODE_ENTER);//KeyEvent.KEYCODE_NUMPAD_ENTER
			
			addEntry(outfile, "NUMDELETE", KeyEvent.KEYCODE_DEL);
			addEntry(outfile, "EXTDELETE", KeyEvent.KEYCODE_DEL);
			addEntry(outfile, "NUMPGDN", KeyEvent.KEYCODE_PAGE_DOWN);
			addEntry(outfile, "EXTPGDN", KeyEvent.KEYCODE_PAGE_DOWN);
			addEntry(outfile, "NUMPGUP", KeyEvent.KEYCODE_PAGE_UP);
			addEntry(outfile, "EXTPGUP", KeyEvent.KEYCODE_PAGE_UP);

			outfile.write(newLine);
			
			addEntry(outfile, "^", "SHIFT+"+ KeyEvent.KEYCODE_6);
			addEntry(outfile, "%", "SHIFT+"+ KeyEvent.KEYCODE_5);
			addEntry(outfile, "~", "SHIFT+"+ KeyEvent.KEYCODE_GRAVE);
			addEntry(outfile, "+", "SHIFT+"+ KeyEvent.KEYCODE_EQUALS);
			addEntry(outfile, "{", "SHIFT+"+ KeyEvent.KEYCODE_LEFT_BRACKET);
			addEntry(outfile, "}", "SHIFT+"+ KeyEvent.KEYCODE_RIGHT_BRACKET);
			addEntry(outfile, "(", "SHIFT+"+ KeyEvent.KEYCODE_9);
			addEntry(outfile, ")", "SHIFT+"+ KeyEvent.KEYCODE_0);

			//close file
			outfile.flush();
			outfile.close();	
		}
		catch(UnsupportedEncodingException x){
			System.out.println(x);
			x.printStackTrace();
		}
		catch(FileNotFoundException x){
			System.out.println(x);
			x.printStackTrace();
		}
		catch(IOException x){
			System.out.println(x);
			x.printStackTrace();
		}
	}
}

