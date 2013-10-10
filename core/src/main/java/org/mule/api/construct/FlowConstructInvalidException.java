/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.construct;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class FlowConstructInvalidException extends MuleException
{

    private static final long serialVersionUID = -8170840339166473625L;

    public FlowConstructInvalidException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public FlowConstructInvalidException(Message message)
    {
        super(message);
    }

    public FlowConstructInvalidException(Message message, FlowConstruct flowConstruct)
    {
        super(message);
        addInfo("FlowConstruct", flowConstruct);
    }

    public FlowConstructInvalidException(Throwable cause)
    {
        super(cause);
    }

}
