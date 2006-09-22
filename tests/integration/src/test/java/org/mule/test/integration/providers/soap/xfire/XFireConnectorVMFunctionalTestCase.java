/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.providers.soap.xfire;

import org.mule.test.integration.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
 {
    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/xfire/xfire-" + getTransportProtocol() + "-mule-config.xml";
    }

    protected String getTransportProtocol() {
        return "vm";
    }

    protected String getSoapProvider() {
        return "xfire";
    }
}
