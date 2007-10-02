/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.streaming;

import org.mule.umo.UMOEventContext;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO
 */

public interface StreamingService
{
    /**
     * A service component that passes the inputStream and output stream for an event direcly to the
     * service component.  Note that the InputStream will never be null, but the OutputStream can be null
     * if the transport being used does not provide a response output stream and there is no outbound endpoint
     * defined for this service.
     * @param in
     * @param out
     * @param context
     * @throws Exception
     */
    void call(InputStream in, OutputStream out, UMOEventContext context) throws Exception;
}
