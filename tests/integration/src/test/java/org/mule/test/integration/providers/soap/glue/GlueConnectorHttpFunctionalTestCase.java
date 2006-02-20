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
package org.mule.test.integration.providers.soap.glue;

import org.mule.test.integration.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{
    protected String getSoapProvider() {
        return "glue";
    }

    protected String getTransportProtocol() {
        return "http";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/glue/glue-http-mule-config.xml";
    }

    protected String getWsdlEndpoint() {
        return "http://127.0.0.1:38008/mule/mycomponent.wsdl";        
    }
}
