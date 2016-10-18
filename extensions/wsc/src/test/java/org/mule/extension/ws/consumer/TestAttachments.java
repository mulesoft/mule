/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.consumer;


import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;


@MTOM
@WebService(portName = "TestAttachmentsPort", serviceName = "TestAttachmentsService")
public class TestAttachments {

  @WebResult(name = "result")
  @WebMethod(action = "uploadAttachment")
  public String uploadAttachment(@WebParam(mode = WebParam.Mode.IN, name = "fileName") String fileName,
                                 @WebParam(mode = WebParam.Mode.IN, name = "attachment") DataHandler attachment) {
    try {
      String received = IOUtils.toString(attachment.getInputStream());
      if (received.contains("some content")) {
        return "OK";
      } else {
        return "UNEXPECTED CONTENT: " + received;
      }
    } catch (IOException e) {
      return "ERROR " + e.getMessage();
    }
  }

  @WebResult(name = "attachment")
  @WebMethod(action = "downloadAttachment")
  public DataHandler downloadAttachment(@WebParam(mode = WebParam.Mode.IN, name = "fileName") String fileName) {
    File file = new File(getResourceAsUrl(fileName).getPath());
    return new DataHandler(new FileDataSource(file));
  }

  private URL getResourceAsUrl(String fileName) {
    try {
      return Thread.currentThread().getContextClassLoader().getResource(fileName).toURI().toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
