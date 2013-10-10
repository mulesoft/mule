/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.errorhandler;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>ErrorMessage</code> represents an exception.
 */
public class ErrorMessage
{
    // Bean representing the Exception
    private ExceptionBean exception;
    // The exception itself, in its primitive state
    private Throwable throwable;
    // Properties for this object
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
