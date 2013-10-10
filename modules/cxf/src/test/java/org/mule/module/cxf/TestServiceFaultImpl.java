/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.module.cxf.testmodels.CustomFault;
import org.mule.module.cxf.testmodels.CxfEnabledFaultMessage;

import javax.jws.WebService;

import org.apache.cxf.interceptor.Fault;

@WebService(endpointInterface = "org.mule.module.cxf.TestServiceFault", serviceName = "TestServiceFault")
public class TestServiceFaultImpl implements TestServiceFault
{
    public String sayHi(String name) throws CxfEnabledFaultMessage
    {
        CustomFault fault = new CustomFault();
        fault.setDescription("Custom Exception Message");
        throw new CxfEnabledFaultMessage("Cxf Exception Message", fault);
    }
}

