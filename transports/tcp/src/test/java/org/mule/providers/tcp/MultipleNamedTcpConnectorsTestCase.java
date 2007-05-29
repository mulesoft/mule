/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.tck.FunctionalTestCase;

public class MultipleNamedTcpConnectorsTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "multiple-named-tcp-connectors-test.xml";
    }

    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.
    }
}

