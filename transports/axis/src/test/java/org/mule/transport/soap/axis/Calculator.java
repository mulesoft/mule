/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

public class Calculator implements CalculatorInterface
{
    public long add(long i1, long i2)
    {
        return i1 + i2;
    }

    public int subtract(int i1, int i2)
    {
        return i1 - i2;
    }
}
