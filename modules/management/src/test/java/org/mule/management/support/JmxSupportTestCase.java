/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.agent.JmxAgent;
import org.mule.module.management.mbean.StatisticsService;
import org.mule.module.management.support.JmxModernSupport;

import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;

public class JmxSupportTestCase extends AbstractMuleJmxTestCase
{
    private final String MANAGER_ID = "Test_Instance";
    private final String TEST_DOMAIN = JmxModernSupport.DEFAULT_JMX_DOMAIN_PREFIX + "." + MANAGER_ID;

    @Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        super.configureMuleContext(contextBuilder);

        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setId(MANAGER_ID);
        contextBuilder.setMuleConfiguration(config);
    }

    public void testClashingDomains() throws Exception
    {
        // pre-register the same domain to simulate a clashing domain
        ObjectName name = ObjectName.getInstance(TEST_DOMAIN + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        JmxAgent agent = new JmxAgent();
        agent.setMuleContext(muleContext);
        agent.initialise();
        muleContext.getRegistry().registerAgent(agent);
        muleContext.start();

        List<String> domains = Arrays.asList(mBeanServer.getDomains());
        assertTrue("Should have contained an original domain.", domains.contains(TEST_DOMAIN));
        assertTrue("Should have contained a new domain.", domains.contains(TEST_DOMAIN + ".1"));
    }

    public void testClashingSuffixedDomains() throws Exception
    {

        // get original, pre-test number of domains
        int numOriginalDomains = mBeanServer.getDomains().length;

        // pre-register the same domain to simulate a clashing domain
        ObjectName name = ObjectName.getInstance(TEST_DOMAIN + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        // add another domain with suffix already applied
        name = ObjectName.getInstance(TEST_DOMAIN + ".1" + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        assertEquals("Wrong number of domains created.",
                     numOriginalDomains + 2, mBeanServer.getDomains().length);

        JmxAgent agent = new JmxAgent();
        agent.setMuleContext(muleContext);
        agent.initialise();
        muleContext.getRegistry().registerAgent(agent);
        muleContext.start();

        List<String> domains = Arrays.asList(mBeanServer.getDomains());
        // one extra domain created by Mule's clash resolution
        assertEquals("Wrong number of domains created.",
                     numOriginalDomains + 3, domains.size());

        assertTrue("Should have contained an original domain.", domains.contains(TEST_DOMAIN));
        assertTrue("Should have contained an original suffixed domain.", domains.contains(TEST_DOMAIN + ".1"));
        assertTrue("Should have contained a new domain.", domains.contains(TEST_DOMAIN + ".2"));
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        // This MBean was registered manually so needs to be unregistered manually in tearDown()
        unregisterMBeansByMask(TEST_DOMAIN + ":name=TestDuplicates");
        super.doTearDown();
    }
}
