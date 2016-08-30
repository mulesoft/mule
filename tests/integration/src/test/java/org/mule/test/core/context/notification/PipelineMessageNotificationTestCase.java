/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class PipelineMessageNotificationTestCase extends AbstractNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/pipeline-message-notification-test-flow.xml";
  }

  @Override
  public void doTest() throws Exception {
    MuleClient client = muleContext.getClient();
    assertNotNull(flowRunner("service-1").withPayload("hello sweet world").run());
    expectedException.expect(MessagingException.class);
    assertNotNull(flowRunner("service-2").withPayload("hello sweet world").run());
    assertNotNull(flowRunner("service-3").withPayload("hello sweet world").run());
    flowRunner("service-4").withPayload("goodbye cruel world").asynchronously().run();
    client.request("test://ow-out", RECEIVE_TIMEOUT);
    flowRunner("service-5").withPayload("goodbye cruel world").withInboundProperty("fail", "true").asynchronously().run();
    client.request("test://owException-out", RECEIVE_TIMEOUT);
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        // Request-Response
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
        // Request-Response Request Exception
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
        // Request-Response Response Exception
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
        // One-Way
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
        .serial(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED)
            .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED))
            .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE)))
        // One-Way Request Exception
        .serial(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
        .serial(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED)
            .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED))
            .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE)));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {
    // Nothing to do
  }
}
