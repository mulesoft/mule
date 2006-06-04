/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.providers.file;

import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.file.FileConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectorServiceOverridesTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "test-connector-config.xml";
    }

    public void testServiceOverrides() throws InterruptedException
    {
        // TODO initialised wait?
        Thread.sleep(1000);
        FileConnector c = (FileConnector) MuleManager.getInstance().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());
        assertEquals("org.mule.transformers.simple.ByteArrayToSerializable", c.getServiceOverrides()
                                                                              .get("inbound.transformer"));
        assertNotNull(c.getDefaultInboundTransformer());
        assertNotNull(c.getDefaultOutboundTransformer());
        assertTrue(c.getDefaultInboundTransformer() instanceof ByteArrayToSerializable);
        assertTrue(c.getDefaultOutboundTransformer() instanceof SerializableToByteArray);
    }

    public void testServiceOverrides2() throws InterruptedException
    {
        FileConnector c = (FileConnector) MuleManager.getInstance().lookupConnector("fileConnector1");
        assertNotNull(c);
        assertNull(c.getServiceOverrides());

        c = (FileConnector) MuleManager.getInstance().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());

        c = (FileConnector) MuleManager.getInstance().lookupConnector("fileConnector3");
        assertNotNull(c);
        assertNull(c.getServiceOverrides());
    }

    public void testServiceOverrides3() throws InterruptedException, UMOException
    {
        UMOEndpointURI uri = new MuleEndpointURI("file:///temp");
        UMOEndpoint endpoint = MuleEndpoint.createEndpointFromUri(uri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertNull(((AbstractServiceEnabledConnector) endpoint.getConnector()).getServiceOverrides());

        endpoint = new MuleEndpoint("file:///temp", true);

        FileConnector c = (FileConnector) MuleManager.getInstance().lookupConnector("fileConnector2");
        assertNotNull(c);
        assertNotNull(c.getServiceOverrides());
        endpoint.setConnector(c);

        endpoint.initialise();
        assertNotNull(((AbstractServiceEnabledConnector) endpoint.getConnector()).getServiceOverrides());

        endpoint = new MuleEndpoint("file:///temp?connector=fileConnector3", true);
        endpoint.initialise();
        assertNull(((AbstractServiceEnabledConnector) endpoint.getConnector()).getServiceOverrides());

        endpoint = new MuleEndpoint("file:///temp?connector=fileConnector2", true);
        endpoint.initialise();
        assertNotNull(((AbstractServiceEnabledConnector) endpoint.getConnector()).getServiceOverrides());

    }
}
