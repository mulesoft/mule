/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.testmodels;


public class TestXFireComponent
{
    public String testXFireException(String data) throws XFireEnabledFaultMessage
    {
        CustomFault fault = new CustomFault();
        fault.setDescription("Custom Exception Message");
        throw new XFireEnabledFaultMessage("XFire Exception Message", fault);
    }

    public String testNonXFireException(String data) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Non-XFire Enabled Exception");
    }
}