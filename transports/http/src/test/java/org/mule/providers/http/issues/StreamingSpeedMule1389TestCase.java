/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.issues;

import org.mule.providers.tcp.integration.AbstractStreamingCapacityTestCase;

public class StreamingSpeedMule1389TestCase extends AbstractStreamingCapacityTestCase
{

    public StreamingSpeedMule1389TestCase()
    {
        super(100 * ONE_MB, "tcp://localhost:60210");
    }

    protected String getConfigResources()
    {
        return "streaming-speed-mule-1389.xml";
    }

}
