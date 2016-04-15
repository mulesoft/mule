/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
