/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static java.util.Arrays.asList;
import static org.mule.extension.ws.WscTestUtils.HEADER_IN;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.extension.ws.api.SoapVersion.SOAP11;
import static org.mule.extension.ws.api.SoapVersion.SOAP12;

import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.service.Soap11Service;
import org.mule.extension.ws.service.Soap12Service;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@ArtifactClassLoaderRunnerConfig(plugins = {"org.mule.modules:mule-module-sockets", "org.mule.modules:mule-module-http-ext",
    "org.mule.modules:mule-module-wsc"}, providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractSoapServiceTestCase extends MuleArtifactFunctionalTestCase {

  @ClassRule
  public static DynamicPort servicePort = new DynamicPort("servicePort");

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
    System.setProperty("soapVersion", soapVersion.toString());
    System.setProperty("serviceClass", getServiceClass());
    XMLUnit.setIgnoreWhitespace(true);
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

  public void createWebService() throws Exception {

    // TODO(pablo.kraan): need to remove this property?
    System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME,
                       "org.apache.cxf.bus.CXFBusFactory");
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
      BusFactory.setDefaultBus(bus);
      Endpoint.publish("/server", createServiceInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Object createServiceInstance() throws Exception {
    Class<?> serviceClass = this.getClass().getClassLoader().loadClass(getServiceClass());

    return serviceClass.newInstance();
  }

  @After
  public void tearDown() throws Exception {
    if (httpServer != null) {
      httpServer.stop();
      httpServer.destroy();
    }
  }

  @Override
  protected MuleContext createMuleContext() throws Exception {
    createWebService();
    return super.createMuleContext();
  }
}
