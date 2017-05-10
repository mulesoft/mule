/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap.services;

import static java.util.Arrays.asList;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(portName = "CablePort", serviceName = "CableService")
public class InterdimentionalCableService {

  @WebResult(name = "channel")
  @WebMethod(action = "getChannels")
  public List<String> getChannels() {
    return asList("Two Brothers", "Fake Doors", "The Adventures of Stealy");
  }
}
