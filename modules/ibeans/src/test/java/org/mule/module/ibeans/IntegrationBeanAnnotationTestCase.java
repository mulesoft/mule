/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans;

import org.mule.tck.AbstractMuleTestCase;

import org.ibeans.annotation.IntegrationBean;

public class IntegrationBeanAnnotationTestCase extends AbstractMuleTestCase
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
