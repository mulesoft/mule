/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification.processors;

import static org.junit.Assert.assertNotNull;

import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.config.spring.util.ProcessingStrategyUtils;
import org.mule.context.notification.Node;
import org.mule.context.notification.RestrictedNode;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.runners.Parameterized;

public class HttpMessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase
{

    @Rule
    public DynamicPort proxyPort = new DynamicPort("port");

    @Rule
    public SystemProperty systemProperty;


    public HttpMessageProcessorNotificationTestCase(ConfigVariant variant, String configResources, boolean nonBlocking)
    {
        super(variant, configResources);
        if (nonBlocking)
        {
            systemProperty = new SystemProperty(MuleProperties.MULE_DEFAULT_PROCESSING_STRATEGY,
                                                ProcessingStrategyUtils.NON_BLOCKING_PROCESSING_STRATEGY);
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "org/mule/test/integration/notifications/nonblocking-message-processor" +
                                     "-notification-test-flow.xml", false},
                {ConfigVariant.FLOW, "org/mule/test/integration/notifications/nonblocking-message-processor" +
                                     "-notification-test-flow.xml", true}
        });
    }

    @Override
    public void doTest() throws Exception
    {
        List<String> testList = Arrays.asList("test", "with", "collection");

        MuleClient client = muleContext.getClient();
        assertNotNull(client.send("http://localhost:" + proxyPort.getValue() + "/in", "test", null));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()

                // logger
                .serial(prePost())

                // <response> start
                .serial(pre())

                // logger
                .serial(prePost())

                // request to echo service
                .serial(pre())
                .serial(prePost())
                .serial(post())

                // logger
                .serial(prePost())

                // request to echo service
                .serial(pre())
                .serial(prePost())
                .serial(post())

                // <response> end
                .serial(prePost())
                .serial(post());
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
