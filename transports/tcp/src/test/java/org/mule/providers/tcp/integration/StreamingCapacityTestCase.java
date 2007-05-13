/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

/**
 * This will happily send 1GB while running in significantly less memory, but it takes some time.
 * Since I'd like this to run in CI I will set at 100MB and test memory delta.  But since memory usage
 * could be around that anyway, this is may be a little unreliable.  And there's no way to
 * measure memory use directly in 1.4.  We'll see...
 *
 * IMPORTANT - DO NOT RUN THIS TEST IN AN IDE WITH LOG LEVEL OF DEBUG.  USE INFO TO SEE
 * DIAGNOSTICS.  OTHERWISE THE CONSOLE OUTPUT WILL BE SIMILAR SIZE TO DATA TRANSFERRED,
 * CAUSING CONFUSNG AND PROBABLY FATAL MEMORY USE.
 */
public class StreamingCapacityTestCase extends AbstractStreamingCapacityTestCase
{

    public StreamingCapacityTestCase()
    {
        super(100 * ONE_MB);
    }

    protected String getConfigResources()
    {
        return "tcp-streaming-test.xml";
    }

}

