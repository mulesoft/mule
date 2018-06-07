/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.mbeans;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mule.module.management.agent.JmxServerNotificationAgent.SERVER_NOTIFICATION_SOURCE_NAME;
import static org.mule.module.management.agent.JmxServerNotificationAgent.LISTENER_JMX_OBJECT_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.management.agent.JmxApplicationAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.mbean.ConnectorService;
import org.mule.module.management.mbean.EndpointService;
import org.mule.module.management.mbean.FlowConstructService;
import org.mule.module.management.mbean.FlowConstructStats;
import org.mule.module.management.mbean.ModelService;
import org.mule.module.management.mbean.MuleConfigurationService;
import org.mule.module.management.mbean.MuleService;
import org.mule.module.management.mbean.RouterStats;
import org.mule.module.management.mbean.ServiceService;
import org.mule.module.management.mbean.StatisticsService;
import org.mule.module.management.support.JmxSupport;
import org.mule.tck.AbstractServiceAndFlowTestCase;

/**
 * Verify that expected MBeans are registered based on the config.
 */
public class MBeansRegistrationTestCase extends AbstractServiceAndFlowTestCase
{

    private MBeanServer mBeanServer;
    private String domainName;
    private JmxSupport jmxSupport;

    public MBeansRegistrationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        JmxApplicationAgent jmxAgent = (JmxApplicationAgent) muleContext.getRegistry().lookupAgent("jmx-agent");
        jmxSupport = jmxAgent.getJmxSupportFactory().getJmxSupport();
        domainName = jmxSupport.getDomainName(muleContext);
        mBeanServer = jmxAgent.getMBeanServer();
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.SERVICE, "mbeans-test-service.xml"},
                                             {ConfigVariant.FLOW, "mbeans-test-flow.xml"}});
    }

    /**
     * Verify that all expected MBeans are registered for a default config
     */
    @Test
    public void testDefaultMBeansRegistration() throws Exception
    {
        List<String> mbeanClasses = getMBeanClasses();

        assertThat(mbeanClasses, hasItem(JmxServerNotificationAgent.class.getName()
                                         + "$BroadcastNotificationService"));
        assertThat(mbeanClasses, hasItem(JmxServerNotificationAgent.class.getName() + "$NotificationListener"));
        assertThat(mbeanClasses, hasItem(MuleService.class.getName()));
        assertThat(mbeanClasses, hasItem(MuleConfigurationService.class.getName()));
        assertThat(mbeanClasses, hasItem(StatisticsService.class.getName()));
        assertThat(mbeanClasses, hasItem(ModelService.class.getName()));

        // Only if registerMx4jAdapter="true"
        assertThat(mbeanClasses, hasItem(mx4j.tools.adaptor.http.HttpAdaptor.class.getName()));
    }

    /**
     * Verify that all expected MBeans are registered for connectors, services, routers, and endpoints.
     */
    @Test
    public void testServiceMBeansRegistration() throws Exception
    {
        List<String> mbeanClasses = getMBeanClasses();

        assertThat(mbeanClasses, hasItem(ConnectorService.class.getName()));
        assertThat(mbeanClasses, hasItem(ModelService.class.getName()));

        if (variant.equals(ConfigVariant.SERVICE))
        {
            assertThat(mbeanClasses, hasItem(ServiceService.class.getName()));
            assertThat(mbeanClasses, hasItem(RouterStats.class.getName()));
        }
        else
        {
            assertThat(mbeanClasses, hasItem(FlowConstructService.class.getName()));
            assertThat(mbeanClasses, hasItem(FlowConstructStats.class.getName()));
        }


        assertThat(mbeanClasses, hasItem(EndpointService.class.getName()));
    }

    /**
     * Verify that all MBeans were unregistered during disposal phase.
     */
    @Test
    public void testMBeansUnregistration() throws Exception
    {
        muleContext.dispose();
        assertThat(getMBeanClasses(), empty());
    }

    @Test
    public void testLogNotificationSourceIsIntentedStringOnRegistration() throws Exception
    {
        ObjectName notificationListenerName = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":" + LISTENER_JMX_OBJECT_NAME);
        List<Notification> notifications = (List<Notification>) mBeanServer.getAttribute(notificationListenerName, "NotificationsList");
        for (Notification notification : notifications)
        {
            assertThat((String) notification.getSource(), equalTo(SERVER_NOTIFICATION_SOURCE_NAME));
        }
    }

    protected List<String> getMBeanClasses() throws MalformedObjectNameException
    {
        Set<ObjectInstance> mbeans = getMBeans();
        Iterator<ObjectInstance> it = mbeans.iterator();
        List<String> mbeanClasses = new ArrayList<String>();
        while (it.hasNext())
        {
            mbeanClasses.add(it.next().getClassName());
        }
        return mbeanClasses;
    }

    protected Set<ObjectInstance> getMBeans() throws MalformedObjectNameException
    {
        return mBeanServer.queryMBeans(jmxSupport.getObjectName(domainName + ":*"), null);
    }

}
