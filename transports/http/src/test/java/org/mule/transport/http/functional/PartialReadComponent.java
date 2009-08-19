/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import java.io.InputStream;

public class PartialReadComponent implements Callable 
{
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        InputStream stream = (InputStream) eventContext.getMessage().getPayload(InputStream.class);

        stream.read();
        return "Hello";
    }
}
