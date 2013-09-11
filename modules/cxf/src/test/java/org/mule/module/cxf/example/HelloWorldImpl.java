/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
