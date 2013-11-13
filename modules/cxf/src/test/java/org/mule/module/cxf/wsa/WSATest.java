/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wsa;

import org.mule.example.employee.EmployeeDirectory;
import org.mule.example.employee.EmployeeDirectory_Service;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.junit.Rule;
import org.junit.Test;

public class WSATest extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "wsa-conf.xml";
    }

    @Test
    public void testWSA() throws Exception
    {
        EmployeeDirectory_Service svc = new EmployeeDirectory_Service();

        EmployeeDirectory port = svc.getEmployeeDirectoryPort(new AddressingFeature());
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "http://localhost:" + dynamicPort.getNumber() + "/services/employee");
    }
}


