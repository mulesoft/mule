/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;

/**
 * This fails to work as described in the issue, but isn't HTTP...
 */
public class StreamingDownloadMule1389TestCase extends AbstractStreamingDownloadMule1389TestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "streaming-download-mule-1389-flow.xml";
    }
}
