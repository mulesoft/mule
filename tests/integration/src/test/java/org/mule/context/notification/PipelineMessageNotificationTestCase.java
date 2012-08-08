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
        assertNotNull(client.send("vm://in-1", "hello sweet world", null));
        client.dispatch("vm://in-2", "goodbye cruel world", null);
        client.request("vm://out-2", RECEIVE_TIMEOUT);
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node().serial(
            new Node(PipelineMessageNotification.class, PipelineMessageNotification.REQUEST_PROCESS_BEGIN))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.REQUEST_PROCESS_END))
            .serial(
                new Node(PipelineMessageNotification.class, PipelineMessageNotification.RESPONSE_PROCESS_END).serial(
                    new Node(PipelineMessageNotification.class,
                        PipelineMessageNotification.REQUEST_PROCESS_BEGIN)).serial(
                    new Node(PipelineMessageNotification.class,
                        PipelineMessageNotification.REQUEST_PROCESS_END)));
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
