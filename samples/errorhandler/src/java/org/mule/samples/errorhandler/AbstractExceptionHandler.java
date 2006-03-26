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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;


/**
 *  <code>AbstractExceptionListener</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractExceptionHandler implements ExceptionHandler {

    /** logger used by this class */
    private static transient Log logger =
        LogFactory.getLog(AbstractExceptionHandler.class);

    protected HashMap registry = new HashMap();
    
    private String endpointName;

    protected ErrorManager errorManager = null;

    public void registerException(Class exceptionClass) {

        registry.put(exceptionClass, exceptionClass);

    }

    public Iterator getRegisteredClasses() {
        return registry.keySet().iterator();
    }

    public void unRegisterException(Class exceptionClass) {
        registry.remove(exceptionClass);

    }

    public boolean isRegisteredFor(Class exceptionClass) {
        Class aClass=null;
        for(Iterator i = getRegisteredClasses(); i.hasNext(); ) {
            aClass = (Class)i.next();
            if(aClass.isAssignableFrom(exceptionClass)){
                return true;
            }
        }
        return false;
    }

    public void onException(ErrorMessage message) throws HandlerException {

        Throwable t = null;

        try {
            t = message.getException().toException();
        } catch (Exception e) {
            throw new HandlerException("Failed to retrieve exception from exception message: " + e, e);
        }

        if(!isRegisteredFor(t.getClass())) {
            throw new HandlerException("Exception: " + t.getClass().getName() +
            " was received by Exception behaviour: " + getClass().getName() +
            ", but the exception is not registered to be handled by this behaviour");
        }
        processException(message, t);
    }

    protected abstract void processException(ErrorMessage message, Throwable t) throws HandlerException;

    /**
     * @return Returns the errorManager.
     */
    public ErrorManager getErrorManager()
    {
        return errorManager;
    }

    /**
     * @param errorManager The errorManager to set.
     */
    public void setErrorManager(ErrorManager errorManager)
    {
        this.errorManager = errorManager;
    }
    /**
     * @return Returns the endpointName.
     */
    public String getendpointName()
    {
        return endpointName;
    }

    /**
     * @param endpointName The endpointName to set.
     */
    public void setEndpointName(String endpointName)
    {
        this.endpointName = endpointName;
    }

}
