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

public class FilteringXmlMessageSplitterFunctionalTestCase extends AbstractXmlOutboundFunctionalTestCase
{

    public void testSplit() throws Exception
    {
        doSend("split");
        assertService(SPLITTER_ENDPOINT_PREFIX, 1, SERVICE_SPLITTER);
        assertService(SPLITTER_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
    }

}