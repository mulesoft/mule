package org.mule.management.agents;

import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.mule.tck.AbstractMuleTestCase;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * $Id$
 */
public class Log4jAgentTestCase extends AbstractMuleTestCase
{
    private MBeanServer mBeanServer;

    protected void setUp() throws Exception
    {
        super.setUp();
        // simulate a running environment with Log4j MBean already registered
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

    public void testRedeploy() throws Exception
    {
        mBeanServer.registerMBean(new HierarchyDynamicMBean(), ObjectName.getInstance(Log4jAgent.JMX_OBJECT_NAME));

        Log4jAgent agent = new Log4jAgent();
        agent.initialise();
    }
}
