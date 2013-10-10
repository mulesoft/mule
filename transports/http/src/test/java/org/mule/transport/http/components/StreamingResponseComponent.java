/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
