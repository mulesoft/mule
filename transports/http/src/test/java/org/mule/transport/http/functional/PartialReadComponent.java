/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.transformer.types.DataTypeFactory;

import java.io.InputStream;

public class PartialReadComponent implements Callable
{
    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        InputStream stream = eventContext.getMessage().getPayload(DataTypeFactory.create(InputStream.class));

        stream.read();
        return "Hello";
    }
}
