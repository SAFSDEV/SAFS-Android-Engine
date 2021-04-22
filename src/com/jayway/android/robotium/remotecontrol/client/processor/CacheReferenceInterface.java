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
package com.jayway.android.robotium.remotecontrol.client.processor;

/**
 * Defines the required API for a Processor able to share cached object references.
 * @author CarlNagle
 */
public interface CacheReferenceInterface {

	/**
	 * Retrieve a cached object from the class instance.
	 * @param key -- String key for identifying the object stored in cache.
	 * @param useChain -- true if the instance should search in all chained caches,  
	 * false if only the individual local cache should be searched.
	 * @return -- the object stored in cache identified by the key, or null if not found.
	 */
	public Object getCachedObject(String key, boolean useChain);
	
	/**
	 * Add an instance of CacheReferenceInterface to the set of caches to be chained 
	 * when seeking an object by key.
	 * @param cache
	 */
	public void addCacheReferenceInterface(CacheReferenceInterface cache);

	/**
	 * Remove an instance of CacheReferenceInterface from the set of caches to be chained  
	 * when seeking an object by key.
	 * @param cache
	 */
	public void removeCacheReferenceInterface(CacheReferenceInterface cache);

	/**
	 * Clear the cache of all object and key references.
	 * @param useChain -- true if the instance should clear all chained caches,  
	 * false if only the individual local cache should be cleared.
	 */
	public void clearCache(boolean useChain);
}
