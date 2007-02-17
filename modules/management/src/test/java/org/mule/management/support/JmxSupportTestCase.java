/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.mule.MuleManager;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.agents.JmxAgent;
import org.mule.management.mbeans.StatisticsService;
import org.mule.umo.manager.UMOManager;

import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;

public class JmxSupportTestCase extends AbstractMuleJmxTestCase
{

    public void testClashingDomains() throws Exception
    {
        final String managerId = "Test_Instance";

        // pre-register the same domain to simulate a clashing domain
        final String testDomain = JmxModernSupport.DEFAULT_JMX_DOMAIN_PREFIX + "." + managerId;
        ObjectName name = ObjectName.getInstance(testDomain + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        UMOManager manager = getManager(true);
        manager.setId(managerId);
        JmxAgent agent = new JmxAgent();
        MuleManager.getRegistry().registerAgent(agent);
        manager.start();

        List domains = Arrays.asList(mBeanServer.getDomains());
        assertTrue("Should have contained an original domain.", domains.contains(testDomain));
        assertTrue("Should have contained a new domain.", domains.contains(testDomain + ".1"));
    }

    public void testClashingSuffixedDomains() throws Exception
    {
        final String managerId = "Test_Instance";

        // get original, pre-test number of domains
        int numOriginalDomains = mBeanServer.getDomains().length;

        // pre-register the same domain to simulate a clashing domain
        final String testDomain = JmxModernSupport.DEFAULT_JMX_DOMAIN_PREFIX + "." + managerId;
        ObjectName name = ObjectName.getInstance(testDomain + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        // add another domain with suffix already applied
        name = ObjectName.getInstance(testDomain + ".1" + ":name=TestDuplicates");
        mBeanServer.registerMBean(new StatisticsService(), name);

        assertEquals("Wrong number of domains created.",
                     numOriginalDomains + 2, mBeanServer.getDomains().length);

        UMOManager manager = getManager(true);
        manager.setId(managerId);
        JmxAgent agent = new JmxAgent();
        MuleManager.getRegistry().registerAgent(agent);
        manager.start();

        List domains = Arrays.asList(mBeanServer.getDomains());
        // one extra domain created by Mule's clash resolution
        assertEquals("Wrong number of domains created.",
                     numOriginalDomains + 3, domains.size());

        assertTrue("Should have contained an original domain.", domains.contains(testDomain));
        assertTrue("Should have contained an original suffixed domain.", domains.contains(testDomain + ".1"));
        assertTrue("Should have contained a new domain.", domains.contains(testDomain + ".2"));
    }

    public void testDomainNoManagerIdAndJmxAgentMustFail() throws Exception
    {
        UMOManager manager = getManager(true);
        JmxAgent jmxAgent = new JmxAgent();
        MuleManager.getRegistry().registerAgent(jmxAgent);
        manager.setId(null);
        try
        {
            manager.start();
            fail("Should have failed.");
            // TODO rework the exception, not the best one here
        } catch (IllegalArgumentException e)
        {
            // this form makes code coverage happier
            assertTrue(true);
        }
    }

}
