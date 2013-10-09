/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.testmodels;

import org.apache.cxf.interceptor.Fault;

public class TestCxfComponent
{
    public String testCxfException(String data) throws CxfEnabledFaultMessage
    {
        CustomFault fault = new CustomFault();
        fault.setDescription("Custom Exception Message");
        throw new CxfEnabledFaultMessage("Cxf Exception Message", fault);
    }

    public String testFault(String data)
    {
        throw new Fault(new IllegalArgumentException("Invalid data argument"));
    }

    public String testNonCxfException(String data)
    {
        throw new UnsupportedOperationException("Non-Cxf Enabled Exception");
    }
}
