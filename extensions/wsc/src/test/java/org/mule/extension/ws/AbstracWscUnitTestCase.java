/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws;

import static javax.xml.ws.Endpoint.publish;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.extension.ws.consumer.TestAttachments;
import org.mule.extension.ws.consumer.TestService;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.Endpoint;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class AbstracWscUnitTestCase {

  @ClassRule
  public static DynamicPort operationsPort = new DynamicPort("operationsPort");
  public static DynamicPort attachmentsPort = new DynamicPort("attachmentsPort");

  public static final String OPERATIONS_URL = "http://localhost:" + operationsPort.getValue() + "/test";
  public static final String ATTACHMENTS_URL = "http://localhost:" + attachmentsPort.getValue() + "/attachments";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static Endpoint operationsService;
  private static Endpoint attachmentsService;
  protected WsdlIntrospecter serviceIntrospecter;
  protected WsdlIntrospecter attachmentsServiceIntrospecter;
  protected XmlTypeLoader operationsTypeLoader;
  protected XmlTypeLoader attachmentsTypeLoader;

  @BeforeClass
  public static void startService() {
    operationsService = publish(OPERATIONS_URL, new TestService());
    attachmentsService = publish(ATTACHMENTS_URL, new TestAttachments());
    assertThat(operationsService.isPublished(), is(true));
    assertThat(attachmentsService.isPublished(), is(true));
  }

  @Before
  public void setup() {
    XMLUnit.getIgnoreWhitespace();
    serviceIntrospecter = new WsdlIntrospecter(OPERATIONS_URL + "?wsdl", "TestService", "TestPort");
    attachmentsServiceIntrospecter =
        new WsdlIntrospecter(ATTACHMENTS_URL + "?wsdl", "TestAttachmentsService", "TestAttachmentsPort");
    operationsTypeLoader = new XmlTypeLoader(serviceIntrospecter.getSchemas());
    attachmentsTypeLoader = new XmlTypeLoader(attachmentsServiceIntrospecter.getSchemas());
  }

  @AfterClass
  public static void shutDownService() {
    attachmentsService.stop();
    operationsService.stop();
  }
}
