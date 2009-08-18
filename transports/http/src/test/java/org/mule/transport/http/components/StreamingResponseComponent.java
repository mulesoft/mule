/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import java.io.ByteArrayInputStream;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * A simple component which returns a stream.
 */
public class StreamingResponseComponent implements Callable 
{
    public Object onCall(MuleEventContext eventContext) throws Exception 
    {
        return new ByteArrayInputStream("hello".getBytes());
    }
}
