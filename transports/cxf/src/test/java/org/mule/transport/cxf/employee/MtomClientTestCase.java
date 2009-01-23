/*
 * $Id: MtomClientTestCase.java 11678 2008-05-02 12:03:07Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.employee;

import org.mule.tck.FunctionalTestCase;

public class MtomClientTestCase extends FunctionalTestCase
{

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

    protected String getConfigResources()
    {
        return "mtom-client-conf.xml";
    }

}

