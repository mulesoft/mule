/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.tck.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;

public class AxisConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{
    protected String getTransportProtocol()
    {
        return "vm";
    }

    protected String getSoapProvider()
    {
        return "axis";
    }

    public String getConfigResources()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }
}
