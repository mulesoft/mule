/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JmxDuplicateEndpointNamesTestCase extends AbstractServiceAndFlowTestCase
{

    private List<ObjectInstance> endpointMBeans = new ArrayList<ObjectInstance>();
       
    public JmxDuplicateEndpointNamesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "duplicate-endpoint-addesses-service.xml"},
            {ConfigVariant.FLOW, "duplicate-endpoint-addesses-flow.xml"}
        });
    }      
    
    @Test
    public void testDuplicateNames()
    {
        List<?> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        assertTrue("no local MBean server found", mBeanServers.size() > 0);        
        
        inspectMBeanServers(mBeanServers);
        assertEquals(2, endpointMBeans.size());
    }

    private void inspectMBeanServers(List<?> mBeanServers)
    {
        for (Object o : mBeanServers)
        {
            MBeanServer server = (MBeanServer) o;
            
            Set<?> mBeans = server.queryMBeans(null, null);
            assertTrue("no registered MBeans found", mBeans.size() > 0);
            
            inspectMBeans(mBeans);
        }
    }

    private void inspectMBeans(Set<?> mBeans)
    {
        for (Object o : mBeans)
        {
            ObjectInstance instance = (ObjectInstance) o;
            if (objectNameMatches(instance))
            {
                endpointMBeans.add(instance);
            }
        }
    }

    private boolean objectNameMatches(ObjectInstance instance)
    {
        ObjectName name = instance.getObjectName();
        return name.getCanonicalName().contains("vmInbound");    
    }

}


