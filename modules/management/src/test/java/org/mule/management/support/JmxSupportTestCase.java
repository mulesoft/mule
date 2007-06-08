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

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.agents.JmxAgent;
import org.mule.management.mbeans.StatisticsService;

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

        managementContext.setId(managerId);
        JmxAgent agent = new JmxAgent();
        managementContext.getRegistry().registerAgent(agent);
        managementContext.start();

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

        managementContext.setId(managerId);
        JmxAgent agent = new JmxAgent();
        managementContext.getRegistry().registerAgent(agent);
        managementContext.start();

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
        JmxAgent jmxAgent = new JmxAgent();
        managementContext.getRegistry().registerAgent(jmxAgent);
        try
        {
            managementContext.setId(null);
            fail("Should have failed.");
        }
        catch (IllegalArgumentException e)
        {
            // this form makes code coverage happier
            assertTrue(true);
        }
    }

}
