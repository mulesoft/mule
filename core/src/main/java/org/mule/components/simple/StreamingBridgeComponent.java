/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.components.simple;

import org.mule.impl.model.streaming.StreamingService;
import org.mule.umo.UMOEventContext;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple Bridging component that can be used with the Streaming Model to bridge from one
 * transport stream to another.
 */
public class StreamingBridgeComponent implements StreamingService
{

    public void call(InputStream in, OutputStream out, UMOEventContext context) throws Exception
    {
        if(out==null)
        {
            throw new IllegalStateException("There is no outputstream for this request on: " + context.getEndpointURI() +
            ". This might be because this is a one way request, but the StreamingBridge component should not be used in this scenario");
        }

        IOUtils.copy(in, out);
    }
}
