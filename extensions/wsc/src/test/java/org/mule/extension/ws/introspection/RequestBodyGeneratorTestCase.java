/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.introspection;

import static javax.xml.ws.Endpoint.publish;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS_XML;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.consumer.TestService;
import org.mule.extension.ws.internal.WscConnection;
import org.mule.extension.ws.internal.introspection.RequestBodyGenerator;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.Endpoint;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RequestBodyGeneratorTestCase {

  @ClassRule
  public static DynamicPort port = new DynamicPort("port");

  public static final String TEST_URL = "http://localhost:" + port.getValue() + "/testService";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static WscConnection connection;
  private static Endpoint service;

  @BeforeClass
  public static void addIntrospecter() {
    if (service == null) {
      service = publish(TEST_URL, new TestService());
    }
    assertThat(service.isPublished(), is(true));
    connection = mock(WscConnection.class);
    when(connection.getWsdlIntrospecter()).thenReturn(new WsdlIntrospecter(TEST_URL + "?wsdl", "TestService", "TestPort"));
  }

  @Test
  public void noParams() throws Exception {
    String request = new RequestBodyGenerator().generateRequest(connection, NO_PARAMS);
    assertSimilarXml(request, resourceAsString("request/" + NO_PARAMS_XML));
  }

  @Test
  public void withParams() throws Exception {
    exception.expect(WscException.class);
    exception.expectMessage("a default one cannot be built");
    new RequestBodyGenerator().generateRequest(connection, ECHO);
  }
}
