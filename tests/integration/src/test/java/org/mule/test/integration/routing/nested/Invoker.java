/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.nested;

import java.util.Date;

public class Invoker
{
    private HelloInterface hello;

    public String invoke(String s)
    {
        return "Received: " + hello.hello(s, new Integer(0xC0DE));
    }

    public Object returnNull(Date date)
    {
        return hello.returnNull();
    }
    
    public void setHello(HelloInterface hello)
    {
        this.hello = hello;
    }

    public HelloInterface getHello()
    {
        return hello;
    }
}
