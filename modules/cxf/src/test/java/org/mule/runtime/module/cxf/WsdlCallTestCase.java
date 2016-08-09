/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Rule;
import org.junit.Test;

public class WsdlCallTestCase extends FunctionalTestCase {

  @Rule
  public final DynamicPort jettyPort = new DynamicPort("jettyPort");

  @Rule
  public final DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "wsdl-conf-flow-httpn.xml";
  }

  @Test
  public void testRequestWsdlWithHttp() throws Exception {
    String location = "http://localhost:" + httpPort.getNumber() + "/cxfService";
    InputStream wsdlStream = new URL(location + "?wsdl").openStream();

    Document document = new SAXReader().read(wsdlStream);
    List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
    assertEquals(((Element) nodes.get(0)).attribute("name").getStringValue(), "Callable");

    nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port/soap:address");
    assertEquals(location, ((Element) nodes.get(0)).attribute("location").getStringValue());
  }

}
