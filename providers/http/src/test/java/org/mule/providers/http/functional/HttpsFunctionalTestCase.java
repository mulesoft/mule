/*
 * $Id$ 
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.http.functional;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.HttpsConnector;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpsFunctionalTestCase extends HttpFunctionalTestCase
{
    protected UMOConnector createConnector() throws Exception
    {
        HttpsConnector connector = new HttpsConnector();
        connector.setName("testHttps");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        connector.setKeyStore("serverKeystore");
        connector.setStorePassword("mulepassword");
        connector.setKeyPassword("mulepassword");
        connector.setTrustStore("trustStore");
        connector.setTrustStorePassword("mulepassword");
        return connector;
    }

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("https://localhost:60198");
        }
        catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }
}
