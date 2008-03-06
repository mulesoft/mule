/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.api.interceptor.Invocation;
import org.mule.api.MuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.service.Service;
import org.mule.RequestContext;

public abstract class RequestContextInvocation implements Invocation
{

    public Service getService()
    {
        return RequestContext.getEventContext().getService();
    }

    public MuleEvent getEvent()
    {
        return RequestContext.getEvent();
    }

    public MuleMessage getMessage()
    {
        return getEvent().getMessage();
    }

}
