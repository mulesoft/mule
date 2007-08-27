/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.testmodels;

import javax.xml.namespace.QName;

import org.codehaus.xfire.fault.FaultInfoException;

public class XFireEnabledFaultMessage extends FaultInfoException
{
    CustomFault faultInfo;

    public XFireEnabledFaultMessage(String message, CustomFault fault)
    {
        super(message);
        this.faultInfo = fault;
    }

    public XFireEnabledFaultMessage(String message, Throwable t, CustomFault fault)
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
        return new QName("http://org.mule.providers.soap.xfire.testmodels/XFireTestService/", "XFireEnabledFaultMessage");
    }

}