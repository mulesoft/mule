/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.management.ManagementException;

import javax.management.ObjectName;

/**
 * <code>JmxManagementException</code> is thrown by the Jmx agents if an
 * error occurs while executing an operation
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmxManagementException extends ManagementException
{
    private ObjectName objectName;
    /**
     * @param message the exception message
     */
    public JmxManagementException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public JmxManagementException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public JmxManagementException(String message, ObjectName objectName)
    {
        super(message);
        this.objectName = objectName;
    }

    public JmxManagementException(String message, Throwable cause, ObjectName objectName)
    {
        super(message, cause);
        this.objectName = objectName;
    }

    public ObjectName getObjectName()
    {
        return objectName;
    }
}
