/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.testmodels;

import javax.xml.namespace.QName;

import org.apache.cxf.frontend.FaultInfoException;

public class CxfEnabledFaultMessage extends FaultInfoException
{
    CustomFault faultInfo;

    public CxfEnabledFaultMessage(String message, CustomFault fault)
    {
        super(message);
        this.faultInfo = fault;
    }

    public CxfEnabledFaultMessage(String message, Throwable t, CustomFault fault)
    {
        super(message, t);
        this.faultInfo = fault;
    }

    public CustomFault getFaultInfo()
    {
        return faultInfo;
    }

    public static QName getFaultName()
    {
        return new QName("http://org.mule.module.cxf.testmodels/CxfTestService/",
            "CxfEnabledFaultMessage");
    }

}
