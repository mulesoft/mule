/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        client.dispatch("vm://owException", "goodbye cruel world", null);
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
            // Request-Response
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_REQUEST_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_RESPONSE_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            // Request-Response Request Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_EXCEPTION))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            // Request-Response Response Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_REQUEST_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_EXCEPTION))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END))
            // One-Way
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END)
                .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_REQUEST_END))
            // One-Way Request Exception
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_END)
                .parallel(new Node(PipelineMessageNotification.class, PipelineMessageNotification.PROCESS_EXCEPTION))));

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
