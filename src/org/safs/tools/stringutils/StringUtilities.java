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
/*
 * Created on Feb 21, 2004
 *
 */
package org.safs.tools.stringutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;
import java.util.Vector;

import android.graphics.Rect;

/**
 * @author Jack
 * <br>	Apr 07, 2010	(LeiWang)   Add method formRectangle(): create a Rectangle from a string x,y,w,h
 * <br>	NOV 15, 2011	(LeiWang)    Add methods as getDateString(), getTimeString() etc.
 *                                  to use SimpleDateFormat to convert Date to string.                              
 * <br>	NOV 18, 2011	(LeiWang)    Add static field TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond.
 * <br>	DEC 04, 2012	(LeiWang)    Copy this class from SAFS Project.
 */
public abstract class StringUtilities {
	
	public static final String DATE_FORMAT_DATE_1 = "MM-dd-yyyy";
	public static final String DATE_FORMAT_DATE_2 = "MM/dd/yyyy";
	public static final String DATE_FORMAT_DATE_3 = "MMM dd yyyy";
	//HH represents the military time, 24 hours (from 0 to 23) 
	public static final String DATE_FORMAT_MILITARY_TIME = "HH:mm:ss";
	public static final String DATE_FORMAT_MILITARY_TIME_WITHOUTSECOND = "HH:mm";
	public static final String DATE_FORMAT_MILITARY_DATE_TIME = "MM-dd-yyyy HH:mm:ss";

	//hh represents the am-pm time, 12 hours (from 1 to 12) 
	public static final String DATE_FORMAT_AM_PM_TIME = "hh:mm:ss";
	public static final String DATE_FORMAT_AM_PM_DATE_TIME = "MM-dd-yyyy hh:mm:ss";

	//a represents the AM or PM
	public static final String DATE_FORMAT_a = "a";

	public static final String TIME_OF_AM = "06:00:00";
	public static final String TIME_OF_PM = "18:00:00";
	
	public static final String JAVA_TIME_BASE = "01-01-1970 00:00:00";
	public static final String FILE_TIME_BASE = "01-01-1601 00:00:00";
	
	public static final String[] specialKeys = {"~","+","^","%","(",")","{","}"};
	
	/**
	 * <pre>
	 * TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond contains the difference from 
	 * filetime to javatime in millisecond, it is initialized in the static code of this class.
	 * 
	 * FileTime is a class defined in org.safs.natives.win32.Kernel32
	 * FileTime contains a 64-bit value representing the number of 100-nanosecond intervals since January 1, 1601 (UTC/GMT).
	 * FileTime's unit: 100 nanosecond
	 * FileTime's base: January 1, 1601 00:00:00 (UTC/GMT)
	 *
	 * JavaTime represents a point in time that is time milliseconds after January 1, 1970 00:00:00 (UTC/GMT).
	 * JavaTime's unit: millisecond
	 * JavaTime's base: January 1, 1970 00:00:00 (UTC/GMT)
	 * 
	 * 1, 000, 000 nanoseconds = 1 millisecond
	 * 
	 * As the unit and the base-time are different for FileTime and JavaTime, a conversion is needed.
	 * 
	 * How to convert?
	 * Convert from FileTime to JavaTime:
	 * javatime = filetime/10000 - TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond
	 * 
	 * Convert from JavaTime to FileTime:
	 * filetime = (javatime+TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond)*10000
	 * </pre>
	 * @see org.safs.natives.win32.Kernel32.FileTime
	 * @see org.safs.natives.NativeWrapper#convertFileTimeToJavaTime
	 * @see #JAVA_TIME_BASE
	 * @see #FILE_TIME_BASE
	 * @see #DATE_FORMAT_MILITARY_DATE_TIME
	 */
	public static long TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond = 0;
	
	static{
		//Caculate the static field TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond
		java.util.Date fileTimeBase = getDate(FILE_TIME_BASE,DATE_FORMAT_MILITARY_DATE_TIME);
		java.util.Date javaTimeBase = getDate(JAVA_TIME_BASE,DATE_FORMAT_MILITARY_DATE_TIME);
		TimeBaseDifferenceFromFileTimeToJavaTimeInMillisecond = javaTimeBase.getTime() - fileTimeBase.getTime();
		
	}
	
	/**
	 * Method LTWhitespace. Remove whitespace (space and tab chars) from start of strings
	 * @param	strValue	String to remove leading whitespace (space and tab)
	 * @return				strValue without leading whitespace
	 */
	public static String LTWhitespace(String strValue ) {
		String strTrimmed = strValue.replaceAll("^[ \t]*", "") ;
		return strTrimmed ;
	}

	/**
	 * Method RTWhitespace. Remove whitespace (space and tab chars) from end of strings
	 * @param	strValue	String to remove trailing whitespace (space and tab)
	 * @return				strValue without trailing whitespace
	 */
	public static String RTWhitespace(String strValue ) {
		String strTrimmed = strValue.replaceAll("[ \t]*$", "") ;
		return strTrimmed ;
	}

	/**
	 * Method TWhitespace. Remove whitespace (space and tab chars) from start and end of strings
	 * @param	strValue	String to remove whitespace (space and tab)
	 * @return				strValue without leading or trailing whitespace
	 */
	public static String TWhitespace(String strValue) {
		return RTWhitespace(LTWhitespace(strValue)) ;
	}

	/**
	 * Method charIsInRange.  Return boolean T/F specifying wheather char is numerically between
	 * integers beg and end. ( c &gt;= beg && c &lt;= end ) 
	 * @param c character to check
	 * @param beg lower end of range
	 * @param end higher end of range
	 * @return boolean
	 */
	public static boolean charIsInRange(char c, int beg, int end){
		/* function simplifies testing if a char is within a certain range
		 * without this function, the 'if' line would be used inline
		 */
		boolean retval = false ;
		if( c >= beg && c <= end ) retval = true ;
		return retval ;
	}

	/**
	 * Method getNumSubstrings.  Returns the number of occurances of strSub within strText as an integer
	 * @param strSub substring to count occurances
	 * @param strText string to search for strSub occurances
	 * @return int
	 */
	public static int getNumSubstrings(String strSub, String strText) {
		/* using brute force counting since StringTokenizer
		 * does not want to allow delimiters > 1 character wide
		 */
		int count = 0 ;
		int pos = strText.indexOf(strSub,0) ;

		while ( pos >= 0 ) {
			count++;
			pos = strText.indexOf(strSub, pos + strSub.length()) ;
		}

		return count ;

	}
	
	/**
	 * Method nextCharIsDQ.  Return boolean T/F specifying wheather or not the character
	 * in strText at location+1 is a double quote (")
	 * @param strText String to check
	 * @param location int location to start check
	 * @return boolean
	 */
	public static boolean nextCharIsDQ( String strText, int location) {
		/* since a literal double quote is represented by ""
		 * we need to look forward to next char in string and
		 * find out if it's a double quote
		 */
		boolean nextisdq = false ;
		if ( strText.length() - 1 > location ) {
			if ( strText.charAt(location+1) == '"' ) nextisdq = true ;
		}
		return nextisdq ;
	}

	/**
	 * Method locateQuotedSubStrings.  Return a {@link Vector} that contains
	 * {@link Integer} objects whose intValue specifies the location of a double
	 * quote (") in strText.  Note that double quotes must be paired so there
	 * should be an even number of Integer objects withing the returned Vector.
	 * @param strText String in which to locate the quoted substrings
	 * @return Vector
	 */
	public static Vector locateQuotedSubStrings( String strText ) {

		/* For efficiency, we need to know the locations withing the
		 * input string that are themselves quoted substrings.
		 * we use a vector that holds Integer objects that are the
		 * locations of double quotes, excluding literal double quotes
		 * The Vector wil have no elements if no quoted substrings are found
		 * and should contain an even number of elements (two for each
		 * quoted substring) if quoted substrings are found
		 */

		Vector vQuoteLocs = new Vector() ;

		int pos = strText.indexOf("\"",0) ;
		while ( pos >= 0 ) {
			if( ! StringUtilities.nextCharIsDQ(strText,pos) ) {
				Integer qpos = new Integer(pos) ;
				vQuoteLocs.addElement(qpos) ;
				pos = strText.indexOf("\"", pos + 1) ;
			} else { // literal double quote (DQ) so incr by 2
				pos = strText.indexOf("\"", pos + 2) ;
			}
		}

		return vQuoteLocs ;

	}

	/**
	 * Method isQuoted.  Return boolean T/F specifying wheather or not the string
	 * within strText at int location is quoted (within double quotes)
	 * @param strText String to check
	 * @param location Location to check
	 * @return boolean
	 */
	public static boolean isQuoted(String strText, int location) {

		/* return true or false depending on wheather the character at location
		 * is within a quoted string (note "" is a literal quote not an empty)
		 * example: strText is litterally 'ab""c "def " ghi' locations 7-10 are quoted
		 *                                 0123456789012345
		 */

		Vector vQuoteLocs = StringUtilities.locateQuotedSubStrings(strText) ;
		boolean isQuoted = isQuoted(strText, location, vQuoteLocs) ;
		return isQuoted ;
		
	}

	/**
	 * Method isQuoted.  Return boolean T/F specifying wheather or not the string
	 * within strText at int location is quoted (within double quotes)
	 * @param strText String to check
	 * @param location Location to check
	 * @param vQuoteLocs {@link Vector} containing {@link Integer} objects whose intValue 
	 *         specifies the location of a double quote (") in strText.
	 * @return boolean
	 */
	public static boolean isQuoted(String strText, int location, Vector vQuoteLocs) {

		/* return true or false depending on wheather the character at location
		 * is within a quoted string (note "" is a literal quote not an empty)
		 * example: strText is litterally 'ab""c "def " ghi' locations 7-10 are quoted
		 *                                 0123456789012345
		 *
		 * since we stored the locations of strings we can use the index 
		 * values stored in the Vector vQuoteLocs, which contains Integer
		 * objects whos int values are the location of non-literal doublequotes.
		 * Note that the non-literal double quotes are paired (matched)
		 */
		
		boolean isQuoted = false ;
		/* possible early determination, if vector for quote locs is empty, false 
		 * also if the location is negative, it can not be quoted, false 
		 */
		if( ! ( vQuoteLocs.isEmpty() || location < 0 ) ) {

			/* another possibility for early determination
			/* if location is greater than the last quote, isQuoted = false */
			Integer iLastRightQuote = (Integer)vQuoteLocs.lastElement() ;
			int iLastQuoteLoc = iLastRightQuote.intValue() ;
			if (location > iLastQuoteLoc) {
				isQuoted = false ;
				return isQuoted ;
			}

			/* have to search the vector of quote locations until we find
			 * where the queried location is relative to the known quote locations
			 */
			for(int vec_idx = 0 ; vec_idx < vQuoteLocs.size() ; vec_idx+=2 ) {

				/* Left quote loc */
				Integer iLQuoteLoc = (Integer)vQuoteLocs.elementAt(vec_idx) ;
				int iLeftQuoteLoc = iLQuoteLoc.intValue() ;

			 	/* if location is less than the left quote, isQuoted = false */
				if (location < iLeftQuoteLoc) {
					isQuoted = false ;
					break ;
				}
				
				/* Right quote loc */
				Integer iRQuoteLoc = (Integer)vQuoteLocs.elementAt(vec_idx+1) ;
				int iRightQuoteLoc = iRQuoteLoc.intValue() ;

				/* if location is less than the Right, isQuoted = true
				 * this is effectively between Left and Right due to above if-break logic 
				 */
				if( location <= iRightQuoteLoc ) {
					isQuoted = true ;
					break ;
				}
				
				/* proceed to next quote loc element
				 * loop will be continued until there are no more elements in the
				 * vQuoteLoc vector OR we have determined quoted or not from the above
				 */ 
				
			}
		}
		
		return isQuoted ;
	}

	/**
	 * Method getSubStrings.  Return a {@link Vector} containing {@link String} objects
	 * that are quoted substrings within strText
	 * @param strText The string to find substrings
	 * @return Vector
	 */
	public static Vector getSubStrings(String strText) {
		/* return a vector containing substrings of strText as strings within the vector */
		Vector vQuoteLocs = locateQuotedSubStrings(strText) ;
		Vector vSubStrings = getSubStrings(strText, vQuoteLocs) ;
		return vSubStrings ;
	}

	/**
	 * Method getSubStrings.  Return a {@link Vector} containing {@link String} objects
	 * that are quoted substrings within strText
	 * @param strText The string to find substrings
	 * @param vQuoteLocs {@link Vector} containing {@link Integer} objects whose intValue 
	 *         specifies the location of a double quote (") in strText.
	 * @return Vector
	 */
	public static Vector getSubStrings(String strText, Vector vQuoteLocs){
		/* return a vector containing substrings of strText as strings within the vector */
		/* the vQuoteLocs vector holds the locations of the non literal
		 * double quotes.  all we need to do is create a vector whos elements
		 * are the string withing strText between each vQuoteLoc pairs
		 */
		Vector vSubStrings = new Vector() ;
		for(int vec_idx = 0 ; vec_idx < vQuoteLocs.size() ; vec_idx+=2 ) {
			/* Left quote loc */
			Integer iLQuoteLoc = (Integer)vQuoteLocs.elementAt(vec_idx) ;
			int iLeftQuoteLoc = iLQuoteLoc.intValue() ;

			/* Right quote loc */
			Integer iRQuoteLoc = (Integer)vQuoteLocs.elementAt(vec_idx+1) ;
			int iRightQuoteLoc = iRQuoteLoc.intValue() ;
				
			/* the string between left and right quotes is what we're after */
			vSubStrings.addElement(strText.substring(iLeftQuoteLoc+1,iRightQuoteLoc));

		}
		 
		return vSubStrings ;
		
	}
	
	/**
	 * Method locateNextUnquotedNonWhiteSpace.  Return an int containing the location in strSearch
	 * after ipos where the first non-quoted, non-whitespace character is
	 * @param strSearch
	 * @param ipos
	 * @return int
	 */
	public static int locateNextUnquotedNonWhiteSpace(String strSearch, int ipos) {
		
		int targetpos = strSearch.length() - 1 ;
		for( int idx=ipos; idx < strSearch.length(); idx++ ) {
			if( ! StringUtilities.isQuoted(strSearch,idx) ) {
				if( strSearch.charAt(idx) != ' ' && strSearch.charAt(idx) != '\t' ) {
					targetpos = idx ;
					break ;
				}
			}
		}
		return targetpos ;
	}

	/**
	 * Method locateNextNonWhiteSpace.  Return an int containing the location in strSearch
	 * after ipos where the first non whitespace character is
	 * @param strSearch
	 * @param ipos
	 * @return int
	 */
	public static int locateNextNonWhiteSpace(String strSearch, int ipos) {
		
		int targetpos = strSearch.length() - 1 ;
		for( int idx=ipos; idx < strSearch.length(); idx++ ) {
			if( strSearch.charAt(idx) != ' ' && strSearch.charAt(idx) != '\t' ) {
				targetpos = idx ;
				break ;
			}
		}
		return targetpos ;
	}

	/**
	 * Method locateNextUnquotedSingleChar.  Return the integer location within strSearch
	 * that is first occurance of any of the characters making up strChars
	 * @param strSearch The string to search
	 * @param strChars  A string consisting of single characters to find the first occurance of
	 * @param ipos int position within strSearch to begin search
	 * @return int
	 */
	public static int locateNextUnquotedSingleChar(String strSearch, String strChars, int ipos) {
		/* return the position of the first matching char in strChars within strSearch 
		 * beginning at ipos, ignoring quoted substrings within sExpression
		 */
	 	Vector vQuoteLocs = locateQuotedSubStrings(strSearch) ;
		return locateNextUnquotedSingleChar( strSearch, strChars, ipos, vQuoteLocs) ;
	}

	/**
	 * Method locateNextUnquotedSingleChar.  Return the integer location within strSearch
	 * that is first occurance of any of the characters making up strChars
	 * @param strSearch The string to search
	 * @param strChars  A string consisting of single characters to find the first occurance of
	 * @param ipos int position within strSearch to begin search
	 * @return int
	 * @param vQuoteLocs {@link Vector} containing {@link Integer} objects whose intValue 
	 *         specifies the location of a double quote (") in strText.
	 */
	public static int locateNextUnquotedSingleChar(String strSearch, String strChars, int ipos, Vector vQuoteLocs) {
		/* return the position of the first matching char in strChars within strSearch 
		 * beginning at ipos, ignoring quoted substrings within sExpression
		 */

	 	 int npos= strSearch.length() + 1 ;
	 	 
		 for(int c_idx=0; c_idx < strChars.length(); c_idx++ ) {
		 	String strChar = strChars.substring(c_idx,c_idx+1) ;
		 	int iloc = StringUtilities.locateNextUnquotedSubstring(strSearch,strChar,ipos,vQuoteLocs) ;
		 	if(iloc > -1 && iloc < npos) npos = iloc ;
		 }
		 
		 if(npos > strSearch.length() ) npos = -1 ; // strFind was not found
		 return npos ;

	}

	/**
	 * Method locateNextUnquotedSubstring.  Return the integer location within strSearch
	 * that is first occurance of substring strFind
	 * @param strSearch The string to search
	 * @param strFind The substring to search for
	 * @param ipos int position within strSearch to begin search
	 * @return int
	 */
	public static int locateNextUnquotedSubstring(String strSearch, String strFind, int ipos) {
		/* return the position of strFind within strSearch beginning
		 * at ipos, ignoring quoted substrings within sExpression
		 */
	 	Vector vQuoteLocs = locateQuotedSubStrings(strSearch) ;
		return locateNextUnquotedSubstring(strSearch, strFind, ipos, vQuoteLocs) ;
	}

	/**
	 * Method locateNextUnquotedSubstring.  Return the integer location within strSearch
	 * that is first occurance of substring strFind
	 * @param strSearch The string to search
	 * @param strFind The substring to search for
	 * @param ipos int position within strSearch to begin search
	 * @param vQuoteLocs {@link Vector} containing {@link Integer} objects whose intValue 
	 *         specifies the location of a double quote (") in strText.
	 * @return int
	 */
	public static int locateNextUnquotedSubstring(String strSearch, String strFind, int ipos, Vector vQuoteLocs) {
		/* return the position of strFind within strSearch beginning
		 * at ipos, ignoring quoted substrings within sExpression
		 */
		int npos = strSearch.length() + 1 ;
	 	int iloc = strSearch.indexOf(strFind,ipos) ;
	 	boolean bquoted = isQuoted(strSearch,iloc,vQuoteLocs) ;
		while(bquoted) {
			/* skip ahead to the next strFind location after the next quote
			 * for efficiency, saves checking chars we know are quoted
			 */
			iloc = strSearch.indexOf(strFind,strSearch.indexOf("\"",iloc+1)) ;
			bquoted = isQuoted(strSearch,iloc,vQuoteLocs) ;
		}
		if(iloc > -1 && iloc < npos) npos = iloc ;
		if(npos > strSearch.length()) npos = -1 ; // strFind was not found
		return npos ;
	}
	
	/**
	 * Method locateNextSubstring.  Return the integer location within strSearch
	 * that is first occurance of substring strFind
	 * @param strSearch The string to search
	 * @param strFind The substring to search for
	 * @param ipos int position within strSearch to begin search
	 * @return int
	 */
	public static int locateNextSubstring(String strSearch, String strFind, int ipos) {
		/* return the position of strFind within strSearch beginning
		 * at ipos, ignoring quoted substrings within sExpression
		 */
		int npos = strSearch.length() + 1 ;
	 	int iloc = strSearch.indexOf(strFind,ipos) ;
		if(iloc > -1 && iloc < npos) npos = iloc ;
		if(npos > strSearch.length()) npos = -1 ; // strFind was not found
		return npos ;
	}

	/**
	 * Method removeAllNonQuotedWhitespace.  Return a {@link String} that is strValue with
	 * all non quoted whitespace (space and tab) removed. (keeps whitespace between double quotes)
	 * @param strValue The string to remove non-quoted whitespace
	 * @return String
	 */
	public static String removeAllNonQuotedWhitespace(String strValue) {
		/* return strValue with all non quoted whitespace removed */
	 	Vector vQuoteLocs = locateQuotedSubStrings(strValue) ;
	 	return removeAllNonQuotedWhitespace(strValue, vQuoteLocs) ;
	}

	/**
	 * Method removeAllNonQuotedWhitespace.  Return a {@link String} that is strValue with
	 * all non quoted whitespace (space and tab) removed. (keeps whitespace between double quotes)
	 * @param strValue The string to remove non-quoted whitespace
	 * @param vQuoteLocs {@link Vector} containing {@link Integer} objects whose intValue 
	 *         specifies the location of a double quote (") in strText.
	 * @return String
	 */
	public static String removeAllNonQuotedWhitespace(String strValue, Vector vQuoteLocs) {
		/* return strValue with all non quoted whitespace removed */
		String strResult = "" ;
		for( int idx = 0 ; idx < strValue.length() ; idx++ ) {
			if( ! isQuoted(strValue,idx,vQuoteLocs) ) {
				/* remove the char at idx if it is whitespace (space or tab) */
				if( ! ( strValue.substring(idx,idx+1).equals(" ") || strValue.substring(idx,idx+1).equals("\t") ) ) {
					strResult += strValue.substring(idx,idx+1) ;
				}
			} else {
				strResult += strValue.substring(idx,idx+1) ;
			}
		}
		return strResult ;
	}
	
	/**
	 * Method replaceString.  Return a strSearch with strReplace inserted over the locations
	 * from ibegin to iend
	 * @param strSearch  The string to search
	 * @param strReplace The replacement string
	 * @param ibegin int location to begin replacement
	 * @param iend int location to end replacement
	 * @return String
	 */
	public static String replaceString(String strSearch, String strReplace, int ibegin, int iend) {
		/* replace the substring of strSearch between positions ibegin and iend
		 * with strReplace
		 */
		String strReturn = strSearch.substring(0,ibegin) + strReplace + strSearch.substring(iend+1,strSearch.length()) ;
		return strReturn ;
	}

	/**
	 * Method reverse.  Return string s with the characters making it up from right to left
	 * @param s The string to reverse
	 * @return String
	 */
	public static String reverse(String s) {
		/* recursively called to return a string that is the characters making up String s reversed */
		String reverseString = "" ;
		if( s.length() > 0 )
			reverseString = s.charAt( s.length() - 1 ) + reverse( s.substring( 0, s.length() - 1) ) ;
		return reverseString;
	}

	/**
	 * Method isLegalVarName.  Return a boolean T/F specifying wheather or not strVarName
	 * is a legal variable name within SAFS
	 * @param strVarName The variable name to test
	 * @return boolean
	 */
	public static boolean isLegalVarName(String strVarName) {
		/* . a-z A-Z 0-9 _ are typical chars used in varnames
		 * replace them all with empty to simplify validation
		 * and for efficiency purposes
		 */
		String strNonStdChars = strVarName.replaceAll("[a-zA-Z0-9_\\.]*", "") ;

		/* need to validate the non standard character one by one */
		boolean retval = true ;
		for(int idx = 0; idx < strNonStdChars.length(); idx++) {
		  if( ! isValidNonStdChar(strNonStdChars.charAt(idx))) {
			retval = false ;
			break ;
		  }
		}

		return retval ;
	}

	/**
	 * Method isValidNonStdChar.  Return boolean T/F specifying wheather or not
	 * the char c is a valid non standard character for SAFS variable names
	 * @param c  The char to test
	 * @return boolean
	 */
	public static boolean isValidNonStdChar(char c) {
		/* . a-z A-Z 0-9 _ are typical chars used in varnames
		 * this method checks for allwed chars not from the above
		 */

		/* assume unsupported char unless it matches one of the ranges below */
		boolean retval = false ;

		/* charIsInRange tests to see if c is between the other params inclusive */
		if(StringUtilities.charIsInRange(c,129,130)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,132,135)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,137,139)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,154,159)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,168,168)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,180,181)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,192,214)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,216,246)) {
			retval = true ;
		} else if(StringUtilities.charIsInRange(c,248,255)) {
			retval = true ;
		}

		return retval ;

	}
	
	/**
	 * Replace all instances of search string with the replace string.
	 * This is for pre-Java 1.4
	 */
	public static String findAndReplace(String input, String search, String replace){
		boolean done = false;		

		if((search==null)||(replace==null)) return input;

		try{
			if(search.equals(replace)) return input;
			String newvalue = input;
			while(!done){
				try{
					int index = newvalue.indexOf(search);
					if (index >= 0){
						
						String temp = newvalue.substring(0, index);
						temp += replace;
						temp += newvalue.substring(index + search.length());
						newvalue = temp;
					}
					else { done = true; }
				}
				catch(Exception x){ return input; }				
			}
			return newvalue;
		}
		catch(Exception x){;}
		return input;
	}
	
	/**
	 * Method spacePad.  Returns a string consisting of spaces of length ipad
	 * @param ipad
	 * @return String
	 */
	public static String spacePad(int ipad) {
		String strSpaces = "" ;
		while( ipad > 0 ) {
			strSpaces += " " ;
			ipad-- ;
		}
		return strSpaces ;
	}

	/**
	 * Returns a boolean value based on value of bool.
	 * Returns true if bool is "YES", "TRUE", "ON", "1" or "-1".
	 * Uses bool.equalsIgnoreCase...
	 * @param bool
	 * @return boolean
	 */
	public static boolean convertBool(String bool) {
		
		try{
			if ((bool.equalsIgnoreCase("YES")) ||
			    (bool.equalsIgnoreCase("TRUE"))||
			    (bool.equalsIgnoreCase("ON"))  ||
			    (bool.equalsIgnoreCase("1"))   ||
			    (bool.equalsIgnoreCase("-1")))
			    return true;
		}catch(Exception ex){;}
		return false;
	}
	
	/**
	 * Remove the prefix from the original string, and return the result
	 * <p>Example:<p>
	 * original = "^filename=c:\file.txt" , prefix="^filename="
	 * the result will be "c:\file.txt"
	 * @param original
	 * @param prefix
	 * @return
	 */
	public static String removePrefix(String original,String prefix){
		if(original==null) return "";
		if(prefix==null) return original;
		if(original.equals(prefix)) return "";
		
		if(original.length()>prefix.length()){
			if(original.startsWith(prefix)){
				original = original.substring(prefix.length());
			}
		}
		return original;
	}

	/**
	 * Remove the suffix from the original string, and return the result
	 * <p>Example:<p>
	 * original = "^filename=c:\file.txt" , suffix="=c:\file.txt"
	 * the result will be "^filename"
	 * @param original
	 * @param suffix
	 * @return
	 */
	public static String removeSuffix(String original,String suffix){
		if(original==null) return "";
		if(suffix==null) return original;
		if(original.equals(suffix)) return "";
		
		if(original.length()>suffix.length()){
			if(original.endsWith(suffix)){				
				int l = original.length() - suffix.length();
				original = original.substring(0, l);
			}
		}
		return original;
	}

	/**
	 * Remove any leading and\or trailing double quotes.  
	 * <p>Example:<p>
	 * original = "c:\file.txt" (with quotes)
	 * the result will be 'c:\file.txt'  (no quotes at all)
	 * @param original
	 * @return
	 */
	public static String removeDoubleQuotes(String original){
		original = removePrefix(original, "\"");
		original = removeSuffix(original, "\"");
		return original;
	}
	
	/**
	 * @param value				A string represents an integer value.
	 * @return					An integer converted from string, or null if the conversion fail.
	 * @throws SAFSException
	 */
	public static Integer convertToInteger(String value){
		String debugmsg = StringUtilities.class.getName()+".convertToInteger() ";
		try{
			return Integer.parseInt(value);
		}catch (NumberFormatException e) {
			debug(debugmsg+" can not convert "+value+" to Integer value. "+e.getMessage());
			return null;
		}
	}
	
	/**
	 * 
	 * @param rectangleString		A string represent a rectangle: x, y , widht, height.
	 * @param separator				The separator in rectangleString to separate x y width and height
	 * @return						A java Rectangle object, constructed by rectangleString
	 */
	public static Rect formRectangle(String rectangleString,String separator){
		String debugmsg = StringUtilities.class.getName()+".formRectangle(): ";
		
		Rect rectangle = null;
		StringTokenizer st = new StringTokenizer(rectangleString, separator);
		debug(debugmsg+" Rectangle string is "+rectangleString);
		if(st.countTokens()==4){
			try{
				int x = Integer.parseInt(st.nextToken());
				int y = Integer.parseInt(st.nextToken());
				int w = Integer.parseInt(st.nextToken());
				int h = Integer.parseInt(st.nextToken());
				rectangle = new Rect(x,y,x+w,y+h);
			}catch(NumberFormatException e){
				debug(debugmsg+" some coordinations in Rectangle string are not numberic.");
			}
		}else{
			debug(debugmsg+" Rectangle string does not contains 4 coordinations.");
		}
		return rectangle;
	}
	
	/**
	 * <em>Note</em>		According to the dateFormat, use the SimpleDateFormat to 
	 * 						convert the date object to string
	 * @param date			A java.util.Date object
	 * @param dateFormat	A string represents the date format pattern of SimpleDateFormat 
	 * @return				A string represents the Date object
	 */
	public static String getDateString(java.util.Date date, String dateFormat) {
		String debugmsg = StringUtilities.class.getName() + ".getDateString(): ";
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);

		debug(debugmsg + " format is " + dateFormat);
		return format.format(date);
	}

	/**
	 * <em>Note</em>		With dateFormat "MM-dd-yyyy", use the SimpleDateFormat to 
	 * 						convert the date object to string
	 * @param date			A java.util.Date object
	 * @return				A string represents the Date object's date part
	 */
	public static String getDateString(java.util.Date date) {
		return StringUtilities.getDateString(date,DATE_FORMAT_DATE_1);
	}	

	/**
	 * <em>Note</em>		Use the SimpleDateFormat to convert the date object to string,
	 * 						just keep the time part
	 * @param date			A java.util.Date object
	 * @param isMilitary	A boolean, if true, convert to a military time (24 hours format)
	 * @return				A string represents the Date object's time part
	 */	
	public static String getTimeString(java.util.Date date, boolean isMilitary) {
		String dateFormat = DATE_FORMAT_AM_PM_TIME;
		String debugmsg = StringUtilities.class.getName()+".getTimeString() ";
		
		if(isMilitary){
			dateFormat = DATE_FORMAT_MILITARY_TIME;
			debug(debugmsg+" Getting Military format time");
		}else{
			debug(debugmsg+" Getting AM-PM format time");
		}
		
		return StringUtilities.getDateString(date,dateFormat);
	}	

	/**
	 * <em>Note</em>		Use the SimpleDateFormat to convert the date object to string,
	 * 						keep both the date and the time part
	 * @param date			A java.util.Date object
	 * @param isMilitary	A boolean, if true, convert to a military time (24 hours format)
	 * @return				A string represents the Date object
	 */		
	public static String getDateTimeString(java.util.Date date, boolean isMilitary) {
		String dateFormat = DATE_FORMAT_AM_PM_DATE_TIME;
		String debugmsg = StringUtilities.class.getName()+".getDateTimeString() ";
		
		if(isMilitary){
			dateFormat = DATE_FORMAT_MILITARY_DATE_TIME;
			debug(debugmsg+" Getting Military format time");
		}else{
			debug(debugmsg+" Getting AM-PM format time");
		}
		
		return StringUtilities.getDateString(date,dateFormat);
	}

	/**
	 * <em>Note</em>		According to the dateFormat, use the SimpleDateFormat to 
	 * 						convert a string to java.util.Date
	 * @param date			A string represents a date.
	 * @param dateFormat	A string represents the date format pattern of SimpleDateFormat 
	 * @return				A java.util.Date object
	 */	
	public static java.util.Date getDate(String date, String dateFormat) {
		String debugmsg = StringUtilities.class.getName() + ".getDate(): ";
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		java.util.Date returnDate = null;

		debug(debugmsg + " format is " + dateFormat);
		
		try {
			returnDate = format.parse(date);
		} catch (ParseException e) {
			debug(debugmsg + " can't convert date '" + date+"'");
		}
		
		return returnDate;
	}

	/**
	 * <em>Note</em>		Use the SimpleDateFormat to convert a date string to java.util.Date.
	 * 						This method will try format {@link #DATE_FORMAT_DATE_1}, 
	 *                      {@link #DATE_FORMAT_DATE_2} and {@link #DATE_FORMAT_DATE_3}
	 * 
	 * @param date			A string represents a date.
	 * @return				A java.util.Date object, or null if the string can't be converted.
	 */	
	public static java.util.Date getDate(String date) {
		java.util.Date returnDate = null;
		
		returnDate = getDate(date, DATE_FORMAT_DATE_1);
		if(returnDate==null) returnDate = getDate(date,DATE_FORMAT_DATE_2);
		if(returnDate==null) returnDate = getDate(date,DATE_FORMAT_DATE_3);
		
		return returnDate;
	}
	
	/**
	 * <em>Note</em>		Use the SimpleDateFormat to convert a time string to java.util.Date.
	 * 						This method will try format {@link #DATE_FORMAT_MILITARY_TIME} and
	 *                      {@link #DATE_FORMAT_MILITARY_TIME_WITHOUTSECOND}
	 * 
	 * @param time			A string represents a time.
	 * @return				A java.util.Date object, or null if the string can't be converted.
	 */	
	public static java.util.Date getTime(String time) {
		java.util.Date returnDate = null;
		
		returnDate = getDate(time, DATE_FORMAT_MILITARY_TIME);
		if(returnDate==null) returnDate = getDate(time,DATE_FORMAT_MILITARY_TIME_WITHOUTSECOND);
		
		return returnDate;
	}
	
	public static boolean containsSepcialKeys(String value) {
		boolean contain = false;

		if (value == null || value.equals("")) {
			return false;
		}

		for (int i = 0; i < specialKeys.length; i++) {
			if (value.indexOf(specialKeys[i]) != -1) {
				contain = true;
				break;
			}
		}

		return contain;
	}	
	
	public static void debug(String message){
		System.out.println(message);
	}
	
	public static void main(String[] args) {
		java.util.Date current = null;
		
		System.out.println("***********************  TIME_OF_AM: "+TIME_OF_AM);
		current = getDate(TIME_OF_AM,DATE_FORMAT_MILITARY_TIME);
		
		System.out.println("Military time: "+getTimeString(current,true));
		System.out.println("Am Pm time: "+getTimeString(current,false));
		System.out.println("Military date time: "+getDateTimeString(current,true));
		System.out.println("Am Pm date time: "+getDateTimeString(current,false));
		
		System.out.println("***********************  TIME_OF_PM: "+TIME_OF_PM);
		current = getDate(TIME_OF_PM,DATE_FORMAT_MILITARY_TIME);
		
		System.out.println("Military time: "+getTimeString(current,true));
		System.out.println("Am Pm time: "+getTimeString(current,false));
		System.out.println("Military date time: "+getDateTimeString(current,true));
		System.out.println("Am Pm date time: "+getDateTimeString(current,false));
	}	
}
