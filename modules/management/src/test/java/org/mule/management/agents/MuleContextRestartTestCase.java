/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.agents;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.management.mbean.MBeanServerFactory;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class MuleContextRestartTestCase extends AbstractServiceAndFlowTestCase
{

    public MuleContextRestartTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-context-restart-config-service.xml"},
            {ConfigVariant.FLOW, "mule-context-restart-config-flow.xml"}});
    }

    @Test
    public void testContextRestart() throws Exception
    {
        muleContext.stop();
        checkCleanShutdown();

        // do it again ;)
        muleContext.start();
        muleContext.stop();
        checkCleanShutdown();
    }

    protected void checkCleanShutdown() throws MalformedObjectNameException
    {
        // check there are no leftover mbeans in mule domain
        final String contextId = muleContext.getConfiguration().getId();
        MBeanServer server = MBeanServerFactory.getOrCreateMBeanServer();
        ObjectName oname = ObjectName.getInstance("Mule." + contextId + ":*");
        Set mbeans = server.queryMBeans(oname, null);

        assertEquals("Not all MBeans unregistered on context stop.", 0, mbeans.size());
    }

}
