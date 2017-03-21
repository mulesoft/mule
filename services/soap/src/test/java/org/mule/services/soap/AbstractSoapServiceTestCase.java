/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.custommonkey.xmlunit.XMLUnit.compareXML;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.services.soap.api.SoapVersion.SOAP11;
import static org.mule.services.soap.api.SoapVersion.SOAP12;
import org.mule.runtime.core.util.IOUtils;
import org.mule.services.soap.api.SoapVersion;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.service.Soap11Service;
import org.mule.services.soap.service.Soap12Service;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public abstract class AbstractSoapServiceTestCase {

  public static final String HEADER_IN =
      "<con:headerIn xmlns:con=\"http://service.soap.services.mule.org/\">Header In Value</con:headerIn>";
  public static final String HEADER_INOUT =
      "<con:headerInOut xmlns:con=\"http://service.soap.services.mule.org/\">Header In Out Value</con:headerInOut>";
  public static final String HEADER_INOUT_RES =
      "<con:headerInOut xmlns:con=\"http://service.soap.services.mule.org/\">Header In Out Value INOUT</con:headerInOut>";
  public static final String HEADER_OUT =
      "<con:headerOut xmlns:con=\"http://service.soap.services.mule.org/\">Header In Value OUT</con:headerOut>\n";

  @Rule
  public DynamicPort servicePort = new DynamicPort("servicePort");

  protected String defaultAddress = "http://localhost:" + servicePort.getValue() + "/server";

  @Parameterized.Parameter
  public SoapVersion soapVersion;

  @Parameterized.Parameter(1)
  public String serviceClass;

  private Server httpServer;
  protected TestSoapClient client;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {SOAP11, Soap11Service.class.getName()},
        {SOAP12, Soap12Service.class.getName()}
    });
  }

  @Before
  public void before() throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    createWebService();
    this.client = new TestSoapClient(defaultAddress + "?wsdl", defaultAddress, isMtom(), getSecurityStrategies(), soapVersion);
  }

  protected List<SecurityStrategy> getSecurityStrategies() {
    return emptyList();
  }

  protected boolean isMtom() {
    return false;
  }

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
      Endpoint.publish("/server", createServiceInstance());
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

  @After
  public void tearDown() throws Exception {
    if (httpServer != null) {
      httpServer.stop();
      httpServer.destroy();
    }
  }

  protected void assertSimilarXml(String expected, InputStream resultStream) throws Exception {
    String result = IOUtils.toString(resultStream);
    assertSimilarXml(expected, result);
  }

  protected void assertSimilarXml(String expected, String result) throws Exception {
    Diff diff = compareXML(result, expected);
    if (!diff.similar()) {
      System.out.println("Expected xml is:\n");
      System.out.println(prettyPrint(expected));
      System.out.println("########################################\n");
      System.out.println("But got:\n");
      System.out.println(prettyPrint(result));
    }
    assertThat(diff.similar(), is(true));
  }

  String prettyPrint(String a)
      throws TransformerException, ParserConfigurationException, IOException, SAXException {
    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(a));
    Document doc = db.parse(is);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    //initialize StreamResult with File object to save to file
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);
    return result.getWriter().toString();
  }

}
