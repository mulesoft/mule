/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import static org.junit.Assert.assertNotNull;

import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class PipelineMessageNotificationTestCase extends AbstractNotificationTestCase
{

    public PipelineMessageNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void doTest() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        assertNotNull(client.send("vm://rr", "hello sweet world", null));
        assertNotNull(client.send("vm://rrException", "hello sweet world", null));
        assertNotNull(client.send("vm://rrResponseException", "hello sweet world", null));
        client.dispatch("vm://ow", "goodbye cruel world", null);
        client.request("vm://ow-out", RECEIVE_TIMEOUT);
        client.dispatch("vm://owException", "goodbye cruel world", null);
        client.request("vm://owException-out", RECEIVE_TIMEOUT);
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
            // Request-Response
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
            // Request-Response Request Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
            // Request-Response Response Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
            // One-Way
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
            .serial(
                new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED)
                .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED))
                .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE)))
            // One-Way Request Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_START))
            .serial(
                new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED)
                .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_COMPLETE))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_SCHEDULED))
                .parallel(new Node(AsyncMessageNotification.class, AsyncMessageNotification.PROCESS_ASYNC_COMPLETE)));
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        // TODO Auto-generated method stub
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.FLOW,
            "org/mule/test/integration/notifications/pipeline-message-notification-test-flow.xml"}});
    }

}
