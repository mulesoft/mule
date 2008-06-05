/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.bridge;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import javax.xml.transform.Source;

public class EchoComponent implements Callable
{

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        MuleMessage message = eventContext.getMessage();
        Source s = (Source) message.getPayload();
        return s;
    }

}
