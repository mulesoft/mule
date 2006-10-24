/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.UMOEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * The OutputHandler is a strategy class that is set on the StreamMessageAdapter to
 * defer the writing of the message payload until there is a stream available to
 * write it to.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see org.mule.providers.streaming.StreamMessageAdapter
 */
public interface OutputHandler
{

    /**
     * Write the event payload to the stream. Depending on the underlying transport,
     * attachements and message properties may be written to the stream here too.
     * 
     * @param event the current event
     * @param out the output stream to write to
     * @throws IOException
     */
    public void write(UMOEvent event, OutputStream out) throws IOException;

    /**
     * Used to obtain a set of headers that will be sent with this stream payload.
     * Headers are typically set independently from a stream payload.
     * 
     * @param event the current event
     * @return a Map of headers or an empty map if there are no headers
     */
    public Map getHeaders(UMOEvent event);
}
