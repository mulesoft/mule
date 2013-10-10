/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
