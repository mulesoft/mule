/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import org.mule.umo.UMOException;

import java.io.IOException;

public class RoundRobinXmlSplitterFunctionalTestCase extends AbstractXmlOutboundFunctionalTestCase
{

    public void testSimple() throws UMOException, IOException
    {
        doSend("roundrobin");
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 1, SERVICE_SPLITTER);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN);
    }

    public void testComplex() throws UMOException, IOException
    {
        doSend("roundrobin");
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 1, SERVICE_SPLITTER);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN);
        doSend("roundrobin");
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 3, SERVICE_SPLITTER);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 1, ROUND_ROBIN);
        doSend("roundrobin");
        doSend("roundrobin");
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, SERVICE_SPLITTER);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 3, ROUND_ROBIN);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 1, SERVICE_SPLITTER);
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN);
    }

}
