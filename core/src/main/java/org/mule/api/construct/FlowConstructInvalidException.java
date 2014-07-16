/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
