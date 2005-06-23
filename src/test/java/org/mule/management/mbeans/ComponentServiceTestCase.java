/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.MuleManager;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.AbstractMuleTestCase;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * TODO Extract JMX server code into AbstractMuleJmxTestCase. Update Log4jAgentTestCase. 
 *
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 *
 * $Id$$
 */
public class ComponentServiceTestCase extends AbstractMuleTestCase
{
    private MBeanServer mBeanServer;

    protected void setUp() throws Exception
    {
        super.setUp();
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.size() == 0) {
            MBeanServerFactory.createMBeanServer();
        }
        mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);

    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        // unregister all MBeans
        Set objectInstances = mBeanServer.queryMBeans(ObjectName.getInstance("*.*:*"), null);
        for (Iterator it = objectInstances.iterator(); it.hasNext();)
        {
            ObjectInstance instance = (ObjectInstance) it.next();
            mBeanServer.unregisterMBean(instance.getObjectName());
        }

        mBeanServer = null;
    }

    public void testUndeploy() throws Exception
    {
        final String domainOriginal = "TEST_DOMAIN_1";


        MuleManager manager = (MuleManager) getTestManager();
        final MuleDescriptor descriptor = new MuleDescriptor("TEST_SERVICE");
        descriptor.setImplementation(new Object());
        manager.getModel().registerComponent(descriptor);

        manager.start();

        final ComponentService service = new ComponentService("TEST_SERVICE");
        final ObjectName name = ObjectName.getInstance(domainOriginal + ":type=TEST_SERVICE");
        mBeanServer.registerMBean(service, name);
        Set mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("Unexpected number of components registered in the domain.", 4, mbeans.size());

        mBeanServer.unregisterMBean(name);

        mbeans = mBeanServer.queryMBeans(ObjectName.getInstance(domainOriginal + ":*"), null);

        assertEquals("There should be no MBeans left in the domain", 0, mbeans.size());
    }
}
