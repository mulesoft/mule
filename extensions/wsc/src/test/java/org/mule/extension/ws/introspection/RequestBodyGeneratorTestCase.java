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
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.extension.ws.api.WscConnection;
import org.mule.extension.ws.api.introspection.RequestBodyGenerator;
import org.mule.extension.ws.api.introspection.WsdlIntrospecter;
import org.mule.extension.ws.consumer.TestService;

import javax.xml.ws.Endpoint;

import org.junit.Before;
import org.junit.Test;

public class RequestBodyGeneratorTestCase {

  public static final String TEST_URL = "http://localhost:6043/testService";

  private WscConnection connection;

  @Before
  public void addIntrospecter() {
    connection = mock(WscConnection.class);
    Endpoint service = publish(TEST_URL, new TestService());
    assertThat(service.isPublished(), is(true));
    when(connection.getWsdlIntrospecter()).thenReturn(new WsdlIntrospecter(TEST_URL + "?wsdl", "TestService", "TestPort"));
  }

  @Test
  public void noParams() throws Exception {
    String request = new RequestBodyGenerator().generateRequest(connection, NO_PARAMS);
    assertSimilarXml(request, resourceAsString("request/" + NO_PARAMS + ".xml"));
  }
}
