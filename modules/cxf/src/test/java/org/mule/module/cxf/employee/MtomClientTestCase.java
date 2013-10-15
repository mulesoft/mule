/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.employee;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MtomClientTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "mtom-client-conf.xml";
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

