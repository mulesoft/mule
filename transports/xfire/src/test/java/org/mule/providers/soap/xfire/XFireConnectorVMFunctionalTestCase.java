/*
 * $Id:XFireConnectorVMFunctionalTestCase.java 7586 2007-07-19 04:06:50Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.tck.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;

public class XFireConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{

    public String getConfigResources()
    {
        return "xfire-" + getTransportProtocol() + "-mule-config.xml";
    }

    protected String getTransportProtocol()
    {
        return "vm";
    }

    protected String getSoapProvider()
    {
        return "xfire";
    }

}
