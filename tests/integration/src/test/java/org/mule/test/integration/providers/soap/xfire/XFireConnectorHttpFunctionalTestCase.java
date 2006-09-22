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

import org.mule.test.integration.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{
    protected String getTransportProtocol() {
        return "http";
    }

    protected String getSoapProvider() {
        return "xfire";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/xfire/xfire-" + getTransportProtocol() + "-mule-config.xml";
    }
}
