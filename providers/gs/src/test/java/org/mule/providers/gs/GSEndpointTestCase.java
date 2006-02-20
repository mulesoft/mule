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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @version $Revision$
 */

public class GSEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("gs:rmi://localhost:7856/MyContainer/JavaSpaces");
        assertEquals("rmi", url.getScheme());
        assertEquals("gs", url.getSchemeMetaInfo());
        assertEquals("rmi://localhost:7856/MyContainer/JavaSpaces", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(0, url.getParams().size());
    }
}
