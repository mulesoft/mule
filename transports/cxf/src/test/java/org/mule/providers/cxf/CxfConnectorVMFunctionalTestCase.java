/*
 * $Id: XFireConnectorVMFunctionalTestCase.java 5597 2007-03-15 12:54:56Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.tck.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;

public class CxfConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{

    @Override
    public void testSendAndReceiveComplex() throws Throwable
    {
        // TODO Auto-generated method stub
        super.testSendAndReceiveComplex();
    }

    public String getConfigResources()
    {
        return getTransportProtocol() + "-mule-config.xml";
    }

    protected String getTransportProtocol()
    {
        return "vm";
    }

    protected String getSoapProvider()
    {
        return "cxf";
    }

}
