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
 * This class is used to wrap the errors during processing the messages<br>
 * from remote robotium solo in {@link SoloProcessor}
 * 
 * @author Lei Wang, SAS Institute, Inc
 * @since  Feb 21, 2012
 *
 * @see SoloProcessor
 */
public class ProcessorException extends Exception {

	private static final long serialVersionUID = 1L;

	public ProcessorException() {
		super();
	}

	public ProcessorException(final String detailMessage, final Throwable throwable) {
		super(detailMessage, throwable);
	}

	public ProcessorException(final String detailMessage) {
		super(detailMessage);
	}

	public ProcessorException(final Throwable throwable) {
		super(throwable);
	}

}
