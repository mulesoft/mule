/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.example;

import javax.jws.WebService;

@WebService(endpointInterface = "org.mule.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
public class HelloWorldImpl implements HelloWorld
{

    public String sayHi(String text)
    {
        return "Hello\u2297 " + text;
    }
}
