/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.DefaultExceptionStrategy;
import org.mule.RequestContext;
import org.mule.message.DefaultExceptionPayload;

public class TestExceptionStrategy extends DefaultExceptionStrategy
{
    @Override
    protected void defaultHandler(Throwable t)
    {
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new DefaultExceptionPayload(t));
            setReturnMessage("Ka-boom!");
        }
    }
}
