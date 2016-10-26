/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static javax.xml.ws.Endpoint.publish;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.ws.WscTestUtils.HEADER_IN;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT;
import static org.mule.extension.ws.WscTestUtils.HEADER_INOUT_XML;
import static org.mule.extension.ws.WscTestUtils.HEADER_IN_XML;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.extension.ws.consumer.TestService;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleException;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.Endpoint;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

public abstract class WebServiceConsumerTestCase extends MuleArtifactFunctionalTestCase {

  @ClassRule
  public static DynamicPort port = new DynamicPort("port");

  public static final String SERVICE_URL = "http://localhost:" + port.getValue() + "/testService";

  private static Endpoint service;

  @BeforeClass
  public static void startService() throws MuleException {
    XMLUnit.setIgnoreWhitespace(true);
    service = withContextClassLoader(ClassLoader.getSystemClassLoader(), () -> publish(SERVICE_URL, new TestService()));
    assertTrue(service.isPublished());
  }

  @AfterClass
  public static void stopService() {
    service.stop();
  }

  protected Message runFlowWithRequest(String name, String bodyRequestFileName) throws Exception {
    return flowRunner(name)
        .withPayload(resourceAsString("request/" + bodyRequestFileName))
        .withVariable(HEADER_IN, resourceAsString("request/" + HEADER_IN_XML))
        .withVariable(HEADER_INOUT, resourceAsString("request/" + HEADER_INOUT_XML))
        .run()
        .getMessage();
  }

  protected void assertSoapResponse(String expectedResponseResourceName, String outputResponse) throws Exception {
    String expected = resourceAsString("response/" + expectedResponseResourceName);
    assertSimilarXml(expected, outputResponse);
  }
}
