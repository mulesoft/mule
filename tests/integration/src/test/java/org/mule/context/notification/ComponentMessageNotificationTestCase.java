/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and
 * post notification should be received by listeners.
 */
public class ComponentMessageNotificationTestCase extends AbstractNotificationTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/notifications/component-message-notification-test-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/notifications/component-message-notification-test-flow.xml"}});
    }

    public ComponentMessageNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void doTest() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        assertNotNull(client.send("vm://in-1", "hello sweet world", null));
        client.dispatch("vm://in-2", "goodbye cruel world", null);
        assertNotNull(client.request("vm://out-2", 5000));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node().parallel(
            new Node(ComponentMessageNotification.class, ComponentMessageNotification.COMPONENT_PRE_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_POST_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_PRE_INVOKE))
            .parallel(
                new Node(ComponentMessageNotification.class,
                    ComponentMessageNotification.COMPONENT_POST_INVOKE));
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, ComponentMessageNotification.class,
            ComponentMessageNotification.COMPONENT_PRE_INVOKE,
            ComponentMessageNotification.COMPONENT_POST_INVOKE);
    }
}
