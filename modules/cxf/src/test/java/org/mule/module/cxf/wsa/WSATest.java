/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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


