/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.stats.RouterStatistics;
import org.mule.management.stats.SedaComponentStatistics;

import java.util.Set;

import javax.management.ObjectName;

public class ComponentStatsTestCase extends AbstractMuleJmxTestCase
{
    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";

        // inbound/outbound router statistics are required

        final SedaComponentStatistics statistics = new SedaComponentStatistics("TEST_IN", 0, 0);
        statistics.setInboundRouterStat(new RouterStatistics(RouterStatistics.TYPE_INBOUND));
        statistics.setOutboundRouterStat(new RouterStatistics(RouterStatistics.TYPE_OUTBOUND));
        ComponentStats stats = new ComponentStats(statistics);

        final ObjectName name = ObjectName.getInstance(domainOriginal + ":type=TEST_NAME");
        mBeanServer.registerMBean(stats, name);

        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        // Expecting following mbeans to be registered:
        // 1) org.mule.management.mbeans.ComponentStats@TEST_DOMAIN_1:type=TEST_NAME
        // 2) org.mule.management.mbeans.RouterStats@TEST_DOMAIN_1:type=org.mule.Statistics,component=TEST_IN,router=inbound
        // 3) org.mule.management.mbeans.RouterStats@TEST_DOMAIN_1:type=org.mule.Statistics,component=TEST_IN,router=outbound
        assertEquals("Unexpected components registered in the domain.", 3, mbeans.size());

        mBeanServer.unregisterMBean(name);

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
