/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.xa;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class ResourceManagerException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2710661653314559260L;

    /**
     * 
     */
    public ResourceManagerException()
    {
        super();
    }

    /**
     * @param message
     */
    public ResourceManagerException(Message message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public ResourceManagerException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ResourceManagerException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
