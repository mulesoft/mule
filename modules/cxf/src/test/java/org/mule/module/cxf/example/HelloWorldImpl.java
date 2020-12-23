/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.example;

import org.mule.util.IOUtils;

import javax.jws.WebService;
import java.io.IOException;

@WebService(endpointInterface = "org.mule.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
public class HelloWorldImpl implements HelloWorld
{

    public String sayHi(Object text)
    {
        if("[B".equals(text.getClass().getName())){
            try {
                return "Hello\u2297 " + IOUtils.toString((byte[]) text);
            }catch (IOException e){
                return "Hello";
            }
        }

        return "Hello\u2297 " + text;
    }
}
