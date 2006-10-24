/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>ErrorMessage</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ErrorMessage
{
    private ExceptionBean exception;
    private Throwable throwable;
    private Map properties = new HashMap();

    /**
     *
     */
    public ErrorMessage()
    {
        super();
    }

    public ErrorMessage(ExceptionBean exception) throws InstantiationException
    {
        setException(exception);
    }

    public ErrorMessage(Throwable exception)
    {
        setThrowable(exception);
    }

    public ErrorMessage(Throwable exception, Map props)
    {
        setThrowable(exception);
        setProperties(props);
    }

    /**
     * @return Returns the exception.
     */
    public ExceptionBean getException()
    {
        return exception;
    }

    /**
     * @param exception The exception to set.
     */
    public void setException(ExceptionBean exception) throws InstantiationException
    {
        this.exception = exception;
        throwable = exception.toException();
    }

    /**
     * @return Returns the properties.
     */
    public Map getProperties()
    {
        return properties;
    }

    /**
     * @param properties The properties to set.
     */
    public void setProperties(Map properties)
    {
        this.properties = properties;
    }

    /**
     * @return Returns the throwable.
     */
    public Throwable getThrowable()
    {
        return throwable;
    }

    /**
     * @param throwable The throwable to set.
     */
    public void setThrowable(Throwable throwable)
    {
        this.throwable = throwable;
        exception = new ExceptionBean(throwable);
    }

}
