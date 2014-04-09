/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.tck.junit4.DomainFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class SharedResourcesWithJmxAgentTestCase extends DomainFunctionalTestCase
{

    @Override
    protected String getDomainConfig()
    {
        return "domain/jms-shared-connector.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[]
                {
                        new ApplicationConfig("App1", new String[] {"jmx-app-1.xml"}),
                        new ApplicationConfig("App2", new String[] {"jmx-app-2.xml"})
                };
    }

    @Test
    public void testDomainAndAppsAgentsCreation() throws Exception
    {
        JmxAgent jmxAgent = getMuleContextForDomain().getRegistry().lookupObject(JmxAgent.class);
        MBeanServer mBeanServer = jmxAgent.getMBeanServer();

        Set<ObjectName> objectNames = mBeanServer.queryNames(null, null);

        List<String> endpointsForApp1 = getNames(objectNames, "endpoint.jms.queueApp1");
        Assert.assertEquals(1, endpointsForApp1.size());
        Assert.assertTrue(endpointsForApp1.get(0).contains("flowApp1"));
        Assert.assertFalse(endpointsForApp1.get(0).contains("flowApp2"));

        List<String> endpointsForApp2 = getNames(objectNames, "endpoint.jms.queueApp2");
        Assert.assertEquals(1, endpointsForApp2.size());
        Assert.assertTrue(endpointsForApp2.get(0).contains("flowApp2"));
        Assert.assertFalse(endpointsForApp2.get(0).contains("flowApp1"));

        Assert.assertEquals(1, getNames(objectNames, "flowApp1"));
        Assert.assertEquals(1, getNames(objectNames, "flowApp2"));
    }

    private List<String> getNames(Set<ObjectName> objectNames, String filter)
    {
        List<String> names = new ArrayList<String>();
        for (ObjectName objectName : objectNames)
        {
            String canonicalName = objectName.getCanonicalName();
            if (canonicalName.contains(filter))
            {
                names.add(canonicalName);
            }
        }
        return names;
    }
}
