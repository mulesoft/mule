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
package org.mule.providers.vm;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMEndpointTestCase extends AbstractMuleTestCase
{
    public void testUrlWithConnector() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("vm://localhost/some.queue?createConnector=vmConnector2");
        assertEquals("vm", url.getScheme());
        assertEquals("some.queue", url.getAddress());
        assertNull(url.getEndpointName());
        assertNotNull(url.getConnectorName());
        assertEquals("vmConnector2", url.getConnectorName());
        assertEquals("vm://localhost/some.queue?createConnector=vmConnector2", url.toString());
        assertEquals(1, url.getParams().size());
    }

    public void testUrlWithProvider() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("vm://vmProvider/some.queue");
        assertEquals("vm", url.getScheme());
        assertEquals("some.queue", url.getAddress());
        assertEquals("vmProvider", url.getEndpointName());
        assertEquals("vm://vmProvider/some.queue", url.toString());
        assertEquals(0, url.getParams().size());
    }
}
