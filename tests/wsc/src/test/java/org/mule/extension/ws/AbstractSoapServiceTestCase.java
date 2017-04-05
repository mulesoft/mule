/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.mule.extension.ws.WscTestUtils.HEADER_IN;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.services.soap.api.SoapVersion.SOAP11;
import static org.mule.services.soap.api.SoapVersion.SOAP12;
import org.mule.extension.ws.service.Soap11Service;
import org.mule.extension.ws.service.Soap12Service;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.services.soap.api.SoapVersion;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Rule;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class AbstractSoapServiceTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort servicePort = new DynamicPort("servicePort");

  @Rule
  public SystemProperty humanWsdlPath;

  @Parameterized.Parameter
  public SoapVersion soapVersion;

  @Parameterized.Parameter(1)
  public String serviceClass;

  private Server httpServer;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {SOAP11, Soap11Service.class.getName()},
        {SOAP12, Soap12Service.class.getName()}
    });
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {getConfigurationFile()};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();

    System.setProperty("humanWsdl", currentThread().getContextClassLoader().getResource("wsdl/human.wsdl").getPath());
    System.setProperty("soapVersion", soapVersion.toString());
    System.setProperty("serviceClass", getServiceClass());
    XMLUnit.setIgnoreWhitespace(true);

    createWebService();
  }

  protected Message runFlowWithRequest(String flowName, String requestXmlResourceName) throws Exception {
    return flowRunner(flowName)
        .withPayload(getRequestResource(requestXmlResourceName))
        .withVariable(HEADER_IN, getRequestResource(HEADER_IN))
        .withVariable(HEADER_INOUT, getRequestResource(HEADER_INOUT))
        .run()
        .getMessage();
  }

  protected abstract String getConfigurationFile();

  protected String getServiceClass() {
    return serviceClass;
  }

  private void createWebService() throws Exception {
    try {
      httpServer = new Server(servicePort.getNumber());
      ServletHandler servletHandler = new ServletHandler();
      httpServer.setHandler(servletHandler);

      CXFNonSpringServlet cxf = new CXFNonSpringServlet();
      ServletHolder servlet = new ServletHolder(cxf);
      servlet.setName("server");
      servlet.setForcedPath("/");

      servletHandler.addServletWithMapping(servlet, "/*");

      httpServer.start();

      Bus bus = cxf.getBus();

      Interceptor inInterceptor = buildInInterceptor();
      if (inInterceptor != null) {
        bus.getInInterceptors().add(inInterceptor);
      }
      Interceptor outInterceptor = buildOutInterceptor();
      if (outInterceptor != null) {
        bus.getOutInterceptors().add(outInterceptor);
      }

      BusFactory.setDefaultBus(bus);
      Endpoint.publish("/" + getTestName(), createServiceInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Interceptor buildInInterceptor() {
    return null;
  }

  protected Interceptor buildOutInterceptor() {
    return null;
  }

  private Object createServiceInstance() throws Exception {
    Class<?> serviceClass = this.getClass().getClassLoader().loadClass(getServiceClass());

    return serviceClass.newInstance();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    if (httpServer != null) {
      httpServer.stop();
      httpServer.destroy();
    }

    super.doTearDownAfterMuleContextDispose();
  }

  protected String getTestName() {
    return "server";
  }

}
