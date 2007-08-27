/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis.clientbridge;

/**
 * A server-side component to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public ComplexData executeComplexity(ComplexData input)
    {
        System.err.println("DoSomeWork.executeComplexity(" + input + ")");
        return input;
    }
}
