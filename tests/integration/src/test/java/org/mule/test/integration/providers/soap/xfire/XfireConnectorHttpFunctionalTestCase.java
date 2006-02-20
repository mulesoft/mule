/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.soap.xfire;

import org.mule.test.integration.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XfireConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
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

    public void testLocationUrlInWSDL() throws Exception {
        //Todo currently not working
    }
}
