/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.example;

import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;

@MTOM
@WebService(endpointInterface = "org.mule.compatibility.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
public class HelloWorldMtomImpl implements HelloWorld {

  @Override
  public String sayHi(String text) {
    return "Hello\u2297 " + text;
  }
}
