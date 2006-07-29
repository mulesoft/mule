/**
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.remoting;

/**
 * A server-side component to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public String executeByteArray(byte[] input)
    {
        return executeString(new String(input));
    }

    public String executeString(String input)
    {
        String rval = "You said " + input;
        System.err.println("\n\n" + rval + "\n");
        return rval;
    }

    public ComplexData executeComplexity(ComplexData input)
    {
        System.err.println("DoSomeWork.executeComplexity(" + input + ")");
        input.setSomeString(input.getSomeString() + " Received");
        input.setSomeInteger(new Integer(input.getSomeInteger().intValue() + 1));
        return input;
    }
}
