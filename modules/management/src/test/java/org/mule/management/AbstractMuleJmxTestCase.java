/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.mule.tck.AbstractMuleTestCase;

/**
 * This base test case will create a new <code>MBean Server</code> if necessary,
 * and will clean up any registered MBeans in its <code>tearDown()</code> method.
 */
public class AbstractMuleJmxTestCase extends AbstractMuleTestCase
{
    protected MBeanServer mBeanServer;

    protected void doSetUp() throws Exception
    {
        // simulate a running environment with Log4j MBean already registered
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.size() == 0)
        {
            MBeanServerFactory.createMBeanServer();
        }

        mBeanServer = (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
    }

    protected void doTearDown() throws Exception
    {
        // unregister all MBeans
        Set objectInstances = mBeanServer.queryMBeans(ObjectName.getInstance("*.*:*"), null);
        for (Iterator it = objectInstances.iterator(); it.hasNext();)
        {
            ObjectInstance instance = (ObjectInstance)it.next();
            mBeanServer.unregisterMBean(instance.getObjectName());
        }

        mBeanServer = null;
    }

    public void testDummy()
    {
        // this method only exists to silence the test runner
    }

}
