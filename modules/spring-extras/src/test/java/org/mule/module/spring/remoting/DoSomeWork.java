/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.remoting;

/**
 * A server-side service to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public String executeByteArray(byte[] input)
    {
        return executeString(new String(input));
    }

    public String executeString(String input)
    {
        return "You said " + input;
    }

    public ComplexData executeComplexity(ComplexData input)
    {
        input.setSomeString(input.getSomeString() + " Received");
        input.setSomeInteger(new Integer(input.getSomeInteger().intValue() + 1));
        return input;
    }
}
