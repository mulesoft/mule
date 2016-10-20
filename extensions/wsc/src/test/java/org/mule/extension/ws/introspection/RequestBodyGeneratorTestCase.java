/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.introspection;

import static java.util.Collections.emptyList;
import static javax.xml.ws.Endpoint.publish;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.FAIL;
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

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.xml.ws.Endpoint;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;

public class RequestBodyGeneratorTestCase {

  @ClassRule
  public static DynamicPort port = new DynamicPort("port");

  public static final String TEST_URL = "http://localhost:" + port.getValue() + "/testService";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static WscConnection connection;
  private static Endpoint service;
  private RequestBodyGenerator generator = new RequestBodyGenerator();

  @BeforeClass
  public static void startService() {
    service = publish(TEST_URL, new TestService());
    assertThat(service.isPublished(), is(true));
  }

  @Before
  public void setup() {
    connection = mock(WscConnection.class);
    when(connection.getWsdlIntrospecter()).thenReturn(new WsdlIntrospecter(TEST_URL + "?wsdl", "TestService", "TestPort"));
  }


  @Test
  @Description("Checks the generation of a body request for an operation that don't require any parameters")
  public void noParams() throws Exception {
    String request = generator.generateRequest(connection, NO_PARAMS);
    assertSimilarXml(request, resourceAsString("request/" + NO_PARAMS_XML));
  }

  @Test
  @Description("Checks that the generation of a body request for an operation that require parameters fails")
  public void withParams() throws Exception {
    exception.expect(WscException.class);
    exception.expectMessage("Cannot build default body request for operation [echo], the operation requires input parameters");
    generator.generateRequest(connection, ECHO);
  }

  @Test
  @Description("Checks that the generation of a body request for an operation without a body part fails")
  public void noBodyPart() throws Exception {
    exception.expect(WscException.class);
    exception.expectMessage("No SOAP body defined in the WSDL for the specified operation");

    // Makes that the introspecter returns an Binding Operation without input SOAP body.
    WsdlIntrospecter introspecter = mock(WsdlIntrospecter.class);
    BindingOperation bop = mock(BindingOperation.class);
    BindingInput bi = mock(BindingInput.class);
    when(bi.getExtensibilityElements()).thenReturn(emptyList());
    when(bop.getBindingInput()).thenReturn(bi);
    when(introspecter.getBindingOperation(anyString())).thenReturn(bop);
    when(connection.getWsdlIntrospecter()).thenReturn(introspecter);

    generator.generateRequest(connection, FAIL);
  }

  @AfterClass
  public static void shutDownService() {
    service.stop();
  }
}
