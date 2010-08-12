/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wsa;

import org.mule.example.employee.EmployeeDirectory;
import org.mule.example.employee.EmployeeDirectory_Service;
import org.mule.tck.FunctionalTestCase;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

public class WSATest extends FunctionalTestCase
{
    public void testWSA() throws Exception
    {
        EmployeeDirectory_Service svc = new EmployeeDirectory_Service();
        
        EmployeeDirectory port = svc.getEmployeeDirectoryPort(new AddressingFeature());
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
            "http://localhost:63081/services/employee");
        
        System.out.println(port.getEmployees());
        
    }
    
    @Override
    protected String getConfigResources()
    {
        return "wsa-conf.xml";
    }

}


