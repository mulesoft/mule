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

import org.mule.test.integration.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XfireConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
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
