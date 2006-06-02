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
 */
package org.mule.management.agents;

import org.mule.config.i18n.Message;
import org.mule.management.ManagementException;

import javax.management.ObjectName;

/**
 * <code>JmxManagementException</code> is thrown by the Jmx agents if an error
 * occurs while executing an operation
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmxManagementException extends ManagementException
{
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
