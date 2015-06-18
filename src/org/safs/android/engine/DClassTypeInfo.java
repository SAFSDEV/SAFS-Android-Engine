/**
 ** Copyright (C) SAS Institute, All rights reserved.
 ** General Public License: http://www.opensource.org/licenses/gpl-license.php
 */
package org.safs.android.engine;


public class DClassTypeInfo{
	
	public String classname = null;
	public int classindex = 0;
	public int absoluteclassindex = 0;
	public String typeclass = null;
	public int typeindex= 0;
	public int absolutetypeindex = 0;
	public String id = null;
	public int idindex= 0;
	public String name = null;
	public int nameindex= 0;
	public int objectindex = 0;
	public int absoluteobjectindex = 0;

	public DClassTypeInfo(String aclass){
		classname = aclass;
	}
}