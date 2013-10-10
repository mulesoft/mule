/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.employee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MtomClientTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public MtomClientTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mtom-client-conf-service.xml"},
            {ConfigVariant.FLOW, "mtom-client-conf-flow.xml"}
        });
    }      
    
    @Test
    public void testEchoService() throws Exception
    {
        EmployeeDirectoryImpl svc = (EmployeeDirectoryImpl) getComponent("employeeDirectoryService");

        int count = 0;
        while (svc.getInvocationCount() == 0 && count < 5000) {
            count += 500;
            Thread.sleep(500);
        }

        assertEquals(1, svc.getInvocationCount());

        // ensure that an attachment was actually sent.
        assertTrue(AttachmentVerifyInterceptor.HasAttachments);
    }

}

