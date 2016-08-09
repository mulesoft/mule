/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static java.lang.String.valueOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.compatibility.transport.http.HttpConstants.SC_FORBIDDEN;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpExceptionStrategyTestCase extends FunctionalTestCase {

  private static final int TIMEOUT = 3000;

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-exception-strategy-config.xml";
  }

  @Test
  public void testInExceptionDoRollbackHttpSync() throws Exception {
    String url = String.format("http://localhost:%d/flowWithoutExceptionStrategySync", port1.getNumber());
    MuleMessage response = muleContext.getClient().send(url, TEST_MESSAGE, null, TIMEOUT);
    assertThat(response, notNullValue());
    assertThat(response.getPayload(), not(nullValue()));
    assertThat(getPayloadAsString(response), not(TEST_MESSAGE));
    assertThat(response.getExceptionPayload(), notNullValue()); // to be fixed
    assertThat(response.getExceptionPayload(), instanceOf(ExceptionPayload.class)); // to be review/fixed
  }

  @Test
  public void testCustomStatusCodeOnExceptionWithCustomExceptionStrategy() throws Exception {
    String url = String.format("http://localhost:%d/flowWithtCESAndStatusCode", port1.getNumber());
    MuleMessage response = muleContext.getClient().send(url, TEST_MESSAGE, null, TIMEOUT);
    assertThat(response, notNullValue());
    assertThat(response.<String>getInboundProperty("http.status"), is(valueOf(SC_FORBIDDEN)));
  }

  public static class CustomExceptionStrategy extends AbstractMessagingExceptionStrategy {

    @Override
    public MuleEvent handleException(Exception ex, MuleEvent event) {
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(HTTP_STATUS_PROPERTY, valueOf(SC_FORBIDDEN))
          .build());
      return event;
    }
  }
}
