/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.space;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @version $Revision$
 */

public class SpaceEndpointTestCase extends AbstractMuleTestCase
{
    public void testHostPortUrl() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("space:rmi://localhost:7856/MyContainer/JavaSpaces");
        assertEquals("rmi", url.getScheme());
        assertEquals("space", url.getSchemeMetaInfo());
        assertEquals("rmi://localhost:7856/MyContainer/JavaSpaces", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals(0, url.getParams().size());
    }
}
