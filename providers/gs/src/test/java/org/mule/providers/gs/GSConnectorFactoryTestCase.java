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
package org.mule.providers.gs;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class GSConnectorFactoryTestCase extends AbstractMuleTestCase
{
    public void testCreateFromFactory() throws Exception
    {
        MuleEndpointURI url = new MuleEndpointURI(getEndpointURI());
        UMOEndpoint endpoint = ConnectorFactory.createEndpoint(url, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof GSConnector);
        assertTrue(getEndpointURI().endsWith(endpoint.getEndpointURI().toString()));
    }
    
    public String getEndpointURI() {
    	return "gs:rmi://localhoast:1000/MyContainer/JavaSpaces";
    }
}
