/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.config.i18n.Message;
import org.mule.module.management.ManagementException;

import javax.management.ObjectName;

/**
 * <code>JmxManagementException</code> is thrown by the Jmx agents if an error
 * occurs while executing an operation.
 */
public class JmxManagementException extends ManagementException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7912469454512394420L;

    private ObjectName objectName;

    /**
     * @param message the exception message
     */
    public JmxManagementException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public JmxManagementException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message the exception message
     */
    public JmxManagementException(Message message, ObjectName objectName)
    {
        super(message);
        this.objectName = objectName;
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public JmxManagementException(Message message, ObjectName objectName, Throwable cause)
    {
        super(message, cause);
        this.objectName = objectName;

    }

    public JmxManagementException(Throwable cause)
    {
        super(cause);
    }

    public ObjectName getObjectName()
    {
        return objectName;
    }
}
