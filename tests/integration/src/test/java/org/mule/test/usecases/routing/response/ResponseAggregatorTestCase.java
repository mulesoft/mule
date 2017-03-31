/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.runtime.core.routing.requestreply.AbstractAsyncRequestReplyRequester;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class ResponseAggregatorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/routing/response/response-router-flow.xml";
  }

  @Test
  public void testSyncResponse() throws Exception {
    HttpRequest request = HttpRequest.builder().setUri(format("http://localhost:%s", port.getNumber()))
        .setEntity(new ByteArrayHttpEntity("request".getBytes())).setMethod(POST).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    String payload = IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream());
    assertThat(payload, is("Received: request"));
  }

  @Test
  public void testResponseEventsCleanedUp() throws Exception {
    RelaxedAsyncReplyMP mp = new RelaxedAsyncReplyMP();

    try {
      Event event =
          eventBuilder().message(of("message1")).groupCorrelation(new GroupCorrelation(1, null)).build();

      SensingNullMessageProcessor listener = getSensingNullMessageProcessor();
      mp.setListener(listener);
      mp.setReplySource(listener.getMessageSource());

      mp.process(event);

      Map<String, Event> responseEvents = mp.getResponseEvents();
      assertTrue("Response events should be cleaned up.", responseEvents.isEmpty());
    } finally {
      mp.stop();
    }
  }

  /**
   * This class opens up the access to responseEvents map for testing
   */
  private static final class RelaxedAsyncReplyMP extends AbstractAsyncRequestReplyRequester {

    private RelaxedAsyncReplyMP() throws MuleException {
      store = new SimpleMemoryObjectStore<>();
      name = "asyncReply";
      start();
    }

    public Map<String, Event> getResponseEvents() {
      return responseEvents;
    }
  }
}
