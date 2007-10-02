/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

/**
 * This fails to work as described in the issue, but isn't HTTP...
 */
public abstract class StreamingDownloadMule1389TestCase extends AbstractStreamingDownloadMule1389TestCase
{

    public StreamingDownloadMule1389TestCase()
    {
        super("tcp://localhost:65432");
    }

    protected String getConfigResources()
    {
        return "streaming-download-mule-1389.xml";
    }

}
