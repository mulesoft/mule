/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

