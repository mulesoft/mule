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
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.extension.ws.consumer.TestAttachments;
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

public abstract class AbstractSoapServiceTestCase extends MuleArtifactFunctionalTestCase {

  @ClassRule
  public static DynamicPort servicePort = new DynamicPort("servicePort");

  @ClassRule
  public static DynamicPort attachmentPort = new DynamicPort("attachmentPort");

  public static final String SERVICE_URL = "http://localhost:" + servicePort.getValue() + "/testService";
  public static final String ATTACHMENT_SERVICE_URL = "http://localhost:" + attachmentPort.getValue() + "/testAttachments";

  private static Endpoint service;
  private static Endpoint attachmentService;

  @BeforeClass
  public static void startService() throws MuleException {
    XMLUnit.setIgnoreWhitespace(true);
    service = withContextClassLoader(ClassLoader.getSystemClassLoader(), () -> publish(SERVICE_URL, new TestService()));
    attachmentService = withContextClassLoader(ClassLoader.getSystemClassLoader(),
                                               () -> publish(ATTACHMENT_SERVICE_URL, new TestAttachments()));
    assertTrue(service.isPublished());
    assertTrue(attachmentService.isPublished());
  }

  @AfterClass
  public static void stopService() {
    service.stop();
    attachmentService.stop();
  }

  protected Message runFlowWithRequest(String flowName, String requestXmlResourceName) throws Exception {
    return flowRunner(flowName)
        .withPayload(getRequestResource(requestXmlResourceName))
        .withVariable(HEADER_IN, getRequestResource(HEADER_IN))
        .withVariable(HEADER_INOUT, getRequestResource(HEADER_INOUT))
        .run()
        .getMessage();
  }
}
