/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntegrationBeanAnnotationTestCase extends AbstractMuleContextTestCase
{
    public IntegrationBeanAnnotationTestCase()
    {
        setStartContext(true);
    }

    @IntegrationBean
    private HostIpIBean hostip;

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("test", this);
    }

    @Test
    public void testIBeanInjection() throws Exception
    {
        assertNotNull(hostip);
        String result = hostip.getHostInfo("192.215.42.198");
        assertNotNull(result);
//        System.out.println(result);
//        System.out.println(hostip.getHostInfoName("192.215.42.198"));
        assertTrue("has ip", hostip.hasIp("192.215.42.198"));
    }
}
