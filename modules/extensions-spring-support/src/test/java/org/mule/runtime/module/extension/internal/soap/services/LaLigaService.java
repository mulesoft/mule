/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.soap.services;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.test.soap.extension.LaLigaServiceProvider.LA_LIGA_PORT_A;
import static org.mule.test.soap.extension.LaLigaServiceProvider.LA_LIGA_SERVICE_A;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService(portName = LA_LIGA_PORT_A, serviceName = LA_LIGA_SERVICE_A)
public class LaLigaService {

  @WebResult(name = "team")
  @WebMethod(action = "getTeams")
  public List<String> getTeams() {
    return asList("Barcelona", "Real Madrid", "Atleti");
  }

  @WebResult(name = "message")
  @WebMethod(action = "uploadResult")
  public String uploadResult(@WebParam(name = "result") DataHandler attachment) {
    try {
      String received = IOUtils.toString(attachment.getInputStream());
      if (received.contains("Barcelona Won")) {
        return "Ok";
      } else {
        return format("Unexpected Content: [%s], was expecting [Some Content]", received);
      }
    } catch (IOException e) {
      return "Error: " + e.getMessage();
    }
  }
}
