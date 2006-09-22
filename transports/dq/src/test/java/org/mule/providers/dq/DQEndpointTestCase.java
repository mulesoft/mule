/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.dq;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DQEndpointTestCase extends AbstractMuleTestCase
{
    public void testWithoutLibParam() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("dq://QSYS.LIB/L701QUEUE.DTAQ");
        assertEquals("dq", url.getScheme());
        assertEquals("/QSYS.LIB/L701QUEUE.DTAQ", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1, url.getParams().size());
        assertEquals("QSYS.LIB", url.getParams().getProperty("lib"));
    }

    public void testWithLibParam() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("dq://L701QUEUE.DTAQ?lib=QSYS.LIB");
        assertEquals("dq", url.getScheme());
        assertEquals("/QSYS.LIB/L701QUEUE.DTAQ", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(1, url.getParams().size());
        assertEquals("QSYS.LIB", url.getParams().getProperty("lib"));
    }
}
