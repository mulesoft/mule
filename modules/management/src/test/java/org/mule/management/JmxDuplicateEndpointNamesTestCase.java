/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management;

import org.mule.tck.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

public class JmxDuplicateEndpointNamesTestCase extends FunctionalTestCase
{
    private List<ObjectInstance> endpointMBeans = new ArrayList<ObjectInstance>();
    
    @Override
    protected String getConfigResources()
    {
        return "duplicate-endpoint-addesses.xml";
    }

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


