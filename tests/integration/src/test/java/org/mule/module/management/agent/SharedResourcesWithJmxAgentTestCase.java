/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.functional.junit4.DomainFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Assert;
import org.junit.Test;

public class SharedResourcesWithJmxAgentTestCase extends DomainFunctionalTestCase
{

    public static final String APP1 = "App1";
    public static final String APP2 = "App2";
    public static final String ENDPOINT_JMS_QUEUE_APP1 = "endpoint.jms.queueApp1";
    public static final String ENDPOINT_JMS_QUEUE_APP2 = "endpoint.jms.queueApp2";
    public static final String FLOW_APP1 = "flowApp1";
    public static final String FLOW_APP2 = "flowApp2";
    public static final String TYPE_ENDPOINT = "type=Endpoint";

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
                        new ApplicationConfig(APP1, new String[] {"jmx-app-1.xml"}),
                        new ApplicationConfig(APP2, new String[] {"jmx-app-2.xml"})
                };
    }

    @Test
    public void testDomainAndAppsAgentsCreation() throws Exception
    {
        JmxDomainAgent jmxAgent = getMuleContextForDomain().getRegistry().lookupObject(JmxDomainAgent.class);
        MBeanServer mBeanServer = jmxAgent.getMBeanServer();

        Set<ObjectName> objectNames = mBeanServer.queryNames(null, null);

        Assert.assertEquals(1, getNamesCount(objectNames, ENDPOINT_JMS_QUEUE_APP1));
        Assert.assertEquals(1, getNamesCount(objectNames, ENDPOINT_JMS_QUEUE_APP1, FLOW_APP1));

        Assert.assertEquals(1, getNamesCount(objectNames, ENDPOINT_JMS_QUEUE_APP2));
        Assert.assertEquals(1, getNamesCount(objectNames, ENDPOINT_JMS_QUEUE_APP2, FLOW_APP2));

        Assert.assertEquals(1, getNamesCount(objectNames, FLOW_APP1, TYPE_ENDPOINT));
        Assert.assertEquals(1, getNamesCount(objectNames, FLOW_APP2, TYPE_ENDPOINT));
    }

    private int getNamesCount(Set<ObjectName> objectNames, String... filters)
    {
        List<String> names = new ArrayList<String>();
        for (ObjectName objectName : objectNames)
        {
            String canonicalName = objectName.getCanonicalName();
            if (containsAll(canonicalName, filters))
            {
                names.add(canonicalName);
            }
        }
        return names.size();
    }

    private boolean containsAll(String text, String... filters)
    {
        for (String filter : filters)
        {
            if (!text.contains(filter))
            {
                return false;
            }
        }
        return true;
    }
}
