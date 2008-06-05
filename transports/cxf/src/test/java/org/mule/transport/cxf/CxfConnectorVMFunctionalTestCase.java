/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.tck.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;

public class CxfConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{

    @Override
    public void testRequest() throws Throwable
    {
        // TODO Auto-generated method stub
        super.testRequest();
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
