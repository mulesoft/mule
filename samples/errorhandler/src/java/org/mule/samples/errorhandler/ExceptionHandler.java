/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
 
package org.mule.samples.errorhandler;

import java.util.Iterator;

/**
 *  <code>ExceptionHandler</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface ExceptionHandler {

    public ErrorManager getErrorManager();
    
    public void setErrorManager(ErrorManager errorManager);
	
	public void onException(ErrorMessage message) throws HandlerException;
	
	public void registerException(Class exceptionClass);
	
	public void unRegisterException(Class exceptionClass);
	
	public Iterator getRegisteredClasses();
	
	public boolean isRegisteredFor(Class exceptionClass);
	
	
}
