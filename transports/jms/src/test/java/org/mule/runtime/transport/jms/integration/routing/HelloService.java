/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration.routing;

public class HelloService implements HelloInterface
{
    @Override
    public String hello(String s, Integer v)
    {
        return "Hello " + s + " " + v;
    }

    @Override
    public Object returnNull()
    {
        return null;
    }
}
