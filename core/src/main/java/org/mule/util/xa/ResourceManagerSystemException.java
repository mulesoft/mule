/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.xa;

import org.mule.config.i18n.Message;

public class ResourceManagerSystemException extends ResourceManagerException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 1202058044460490597L;

    /**
     * 
     */
    public ResourceManagerSystemException()
    {
        super();
    }

    /**
     * @param message
     */
    public ResourceManagerSystemException(Message message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public ResourceManagerSystemException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ResourceManagerSystemException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
