/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.FunctionalTestCase;

public class MultipleNamedTcpConnectorsTestCase extends DynamicPortTestCase
{
    protected String getConfigResources()
    {
        return "multiple-named-tcp-connectors-test.xml";
    }

    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}

