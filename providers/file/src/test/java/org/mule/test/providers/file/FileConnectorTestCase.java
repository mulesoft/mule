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
 *
 */

package org.mule.test.providers.file;

import com.mockobjects.dynamic.Mock;
import org.mule.providers.file.FileConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;

import java.io.File;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileConnectorTestCase extends AbstractConnectorTestCase
{
    /* (non-Javadoc)
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        UMOConnector connector = new FileConnector();
        connector.setName("testFile");
        connector.initialise();
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "file:///tmp";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testDispatch()
     */
    public void testDispatch() throws Exception
    {
        UMOConnector connector = getConnector();

        Mock session = getMockSession();
        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOComponent component = getTestComponent(descriptor);
        UMOEvent event = getTestEvent("TestData");

        connector.registerListener(component, endpoint);
        connector.start();
        connector.getDispatcher("dummy").dispatch(event);

        session.verify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testSend()
     */
    public void testSend() throws Exception
    {
        UMOConnector connector = getConnector();

        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEvent event = getTestEvent("TestData");
        UMOComponent component = getTestComponent(descriptor);

        connector.registerListener(component, endpoint);
        connector.start();
        connector.getDispatcher("dummy").send(event);
    }

    public Object getValidMessage() throws Exception
    {
        return File.createTempFile("simple", ".mule");
    }
}
