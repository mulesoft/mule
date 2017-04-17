/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestHttpSoapServer {

  private final Server httpServer;
  private final String defaultAddress;
  private final Interceptor in;
  private final Interceptor out;
  private final Object serviceInstance;

  public TestHttpSoapServer(int port, Object serviceInstance) {
    this(port, null, null, serviceInstance);
  }

  public TestHttpSoapServer(int port, Interceptor in, Interceptor out, Object serviceInstance) {
    this.httpServer = new Server(port);
    this.defaultAddress = "http://localhost:" + port + "/server";
    this.in = in;
    this.out = out;
    this.serviceInstance = serviceInstance;
  }

  public void init() throws Exception {
    try {
      ServletHandler servletHandler = new ServletHandler();
      httpServer.setHandler(servletHandler);

      CXFNonSpringServlet cxf = new CXFNonSpringServlet();
      ServletHolder servlet = new ServletHolder(cxf);
      servlet.setName("server");
      servlet.setForcedPath("/");

      servletHandler.addServletWithMapping(servlet, "/*");

      httpServer.start();

      Bus bus = cxf.getBus();

      if (in != null) {
        bus.getInInterceptors().add(in);
      }
      if (out != null) {
        bus.getOutInterceptors().add(out);
      }

      BusFactory.setDefaultBus(bus);
      Endpoint.publish("/server", serviceInstance);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop() throws Exception {
    if (httpServer != null) {
      httpServer.stop();
      httpServer.destroy();
    }
  }

  public String getDefaultAddress() {
    return defaultAddress;
  }
}
