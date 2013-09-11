/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
