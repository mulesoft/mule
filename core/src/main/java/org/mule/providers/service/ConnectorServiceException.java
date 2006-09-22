/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.service;

import org.mule.config.i18n.Message;

/**
 * <code>ConnectorServiceException</code> is thrown if a
 * ProviderServicedescriptor has a service error set. This is usually because
 * the endpoint/connector cannot be created from a service descriptor
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ConnectorServiceException extends ConnectorFactoryException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5377271625492627661L;

    /**
     * @param message the exception message
     */
    public ConnectorServiceException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public ConnectorServiceException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
