/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.issues;

import org.mule.transport.tcp.issues.AbstractStreamingDownloadMule1389TestCase;

/**
 * This fails to work as described in the issue.  We need more info.
 */
public class StreamingDownloadMule1389TestCase extends AbstractStreamingDownloadMule1389TestCase
{

    protected String getConfigResources()
    {
        return "streaming-download-mule-1389.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

}
