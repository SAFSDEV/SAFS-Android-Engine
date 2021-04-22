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
