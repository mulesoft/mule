/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.vm.VMConnector;

import org.junit.Rule;

public class AxisConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getTransportProtocol()
    {
        return VMConnector.VM;
    }

    @Override
    protected String getSoapProvider()
    {
        return "axis";
    }

    @Override
    public String getConfigFile()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }
}
