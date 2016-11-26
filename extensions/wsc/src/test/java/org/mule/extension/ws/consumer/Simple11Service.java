/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.consumer;

import static java.lang.String.format;
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
import javax.xml.ws.Holder;
import javax.xml.ws.soap.MTOM;

/**
 * Web service11 used by WS Consumer tests.
 *
 * @since 4.0
 */
@MTOM
@WebService(portName = "TestPort", serviceName = "TestService")
public class Simple11Service {

  @WebResult(name = "text")
  @WebMethod(action = "echoOperation")
  public String echo(@WebParam(name = "text") String s) {
    return s + " response";
  }

  @WebResult(name = "text")
  @WebMethod(action = "fail")
  public String fail(@WebParam(name = "text") String s) throws EchoException {
    throw new EchoException(s);
  }

  @WebResult(name = "text")
  @WebMethod(action = "echoWithHeaders")
  public String echoWithHeaders(@WebParam(name = "headerIn", header = true, mode = WebParam.Mode.IN) String headerIn,
                                @WebParam(name = "headerOut", header = true, mode = WebParam.Mode.OUT) Holder<String> headerOut,
                                @WebParam(name = "headerInOut", header = true,
                                    mode = WebParam.Mode.INOUT) Holder<String> headerInOut,
                                @WebParam(name = "text") String s)
      throws EchoException {

    if (headerIn == null || headerInOut == null) {
      throw new EchoException("Missing Required Headers");
    }

    headerOut.value = headerIn + " OUT";
    headerInOut.value = headerInOut.value + " INOUT";
    return echo(s);
  }

  @WebResult(name = "text")
  @WebMethod(action = "noParams")
  public String noParams() {
    return "response";
  }

  @WebResult(name = "text")
  @WebMethod(action = "noParams")
  public String noParamsWithHeader(@WebParam(name = "headerIn", header = true, mode = WebParam.Mode.IN) String header) {
    return header;
  }

  @WebResult(name = "account")
  @WebMethod(action = "echoAccount")
  public Account echoAccount(@WebParam(name = "account") Account account, @WebParam(name = "name") String accountName) {
    Account a = new Account();
    a.setClientName(accountName);
    a.setId(account.getId());
    a.setItems(account.getItems());
    a.setStartingDate(account.getStartingDate());
    return a;
  }

  @WebResult(name = "result")
  @WebMethod(action = "uploadAttachment")
  public String uploadAttachment(@WebParam(mode = WebParam.Mode.IN, name = "attachment") DataHandler attachment) {
    try {
      String received = IOUtils.toString(attachment.getInputStream());
      if (received.contains("Some Content")) {
        return "Ok";
      } else {
        return format("Unexpected Content: [%s], was expecting [Some Content]", received);
      }
    } catch (IOException e) {
      return "Error: " + e.getMessage();
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

  public static void main(String[] args) {

  }
}
