/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
