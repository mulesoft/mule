/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.mbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.management.agent.JmxAgent;
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
        JmxAgent jmxAgent = (JmxAgent) muleContext.getRegistry().lookupAgent("jmx-agent");
        jmxSupport = jmxAgent.getJmxSupportFactory().getJmxSupport();
        domainName = jmxSupport.getDomainName(muleContext);
        mBeanServer = jmxAgent.getMBeanServer();
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mbeans-test-service.xml"},
            {ConfigVariant.FLOW, "mbeans-test-flow.xml"}});
    }

    /**
     * Verify that all expected MBeans are registered for a default config
     */
    @Test
    public void testDefaultMBeansRegistration() throws Exception
    {
        List<String> mbeanClasses = getMBeanClasses();

        assertTrue(mbeanClasses.contains(JmxServerNotificationAgent.class.getName()
                                         + "$BroadcastNotificationService"));
        assertTrue(mbeanClasses.contains(JmxServerNotificationAgent.class.getName() + "$NotificationListener"));
        assertTrue(mbeanClasses.contains(MuleService.class.getName()));
        assertTrue(mbeanClasses.contains(MuleConfigurationService.class.getName()));
        assertTrue(mbeanClasses.contains(StatisticsService.class.getName()));
        assertTrue(mbeanClasses.contains(ModelService.class.getName()));

        // Only if registerMx4jAdapter="true"
        assertTrue(mbeanClasses.contains(mx4j.tools.adaptor.http.HttpAdaptor.class.getName()));
    }

    /**
     * Verify that all expected MBeans are registered for connectors, services,
     * routers, and endpoints.
     */
    @Test
    public void testServiceMBeansRegistration() throws Exception
    {
        List<String> mbeanClasses = getMBeanClasses();

        assertTrue(mbeanClasses.contains(ConnectorService.class.getName()));
        assertTrue(mbeanClasses.contains(ModelService.class.getName()));
        
        if(variant.equals(ConfigVariant.SERVICE))
        {
            assertTrue(mbeanClasses.contains(ServiceService.class.getName()));
            assertTrue(mbeanClasses.contains(RouterStats.class.getName()));
        }
        else
        {
            assertTrue(mbeanClasses.contains(FlowConstructService.class.getName()));            
            assertTrue(mbeanClasses.contains(FlowConstructStats.class.getName()));
        }
               
        
        assertTrue(mbeanClasses.contains(EndpointService.class.getName()));
    }

    /**
     * Verify that all MBeans were unregistered during disposal phase.
     */
    @Test
    public void testMBeansUnregistration() throws Exception
    {
        muleContext.dispose();
        assertEquals("No MBeans should be registered after disposal of MuleContext", 0,
            getMBeanClasses().size());
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
