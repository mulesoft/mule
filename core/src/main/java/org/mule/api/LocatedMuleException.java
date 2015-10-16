/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.config.i18n.Message;
import org.mule.util.ObjectUtils;

/**
 * <code>LocatedMuleException</code> is a general exception that adds context location about the Exception (i.e.: where it occurred in the application).
 */

public class LocatedMuleException extends MuleException
{
    public static final String INFO_LOCATION_KEY = "Element";
    public static final String INFO_SOURCE_XML_KEY = "Element XML";

    /**
     * Serial version
     */
    private static final long serialVersionUID = 6941498759267936649L;

    /**
     * @param component the object that failed during a lifecycle method call
     */
    public LocatedMuleException(Object component)
    {
        super();
        setLocation(component);
    }

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public LocatedMuleException(Message message, Object component)
    {
        super(message);
        setLocation(component);
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LocatedMuleException(Message message, Throwable cause, Object component)
    {
        super(message, cause);
        setLocation(component);
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LocatedMuleException(Throwable cause, Object component)
    {
        super(cause);
        setLocation(component);
    }


    protected void setLocation(Object component)
    {
        if (component != null)
        {
            addInfo(INFO_LOCATION_KEY, resolveProcessorPath(component));
        }
    }

    protected String resolveProcessorPath(Object component)
    {
        if (component instanceof NamedObject)
        {
            // Cannot currently get the application name without an event/context.
            return LocationExecutionContextProvider.resolveProcessorRepresentation("app", "/" + ((NamedObject) component).getName(), component);
        }
        else
        {
            return LocationExecutionContextProvider.resolveProcessorRepresentation("app", ObjectUtils.toString(component, "null"), component);
        }
    }
}
