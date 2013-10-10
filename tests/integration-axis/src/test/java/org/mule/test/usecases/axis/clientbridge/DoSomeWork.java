/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis.clientbridge;

/**
 * A server-side service to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public ComplexData executeComplexity(ComplexData input)
    {
        System.err.println("DoSomeWork.executeComplexity(" + input + ")");
        return input;
    }
}
