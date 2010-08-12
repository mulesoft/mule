/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.testmodels;

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
        return new QName("http://org.mule.transport.cxf.testmodels/CxfTestService/",
            "CxfEnabledFaultMessage");
    }

}
