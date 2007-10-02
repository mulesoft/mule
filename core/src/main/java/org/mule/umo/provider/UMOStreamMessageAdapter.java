/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.provider;

import org.mule.umo.UMOEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A stream message adapter rovides a generic base class for stream based message
 * flows in Mule. This adapter represents the 3 flows of data that Mule identifies,
 * namely inbound, outbound and response flows. These are represented by three
 * streams on the adapter.
 */

public interface UMOStreamMessageAdapter extends UMOMessageAdapter
{
    /**
     * Gets the input Stream associated with this event
     * 
     * @return the input Stream associated with this event
     */
    InputStream getInputStream();

    /**
     * Gets the output Stream associated with this event
     * 
     * @return the output Stream associated with this event
     */
    OutputStream getOutputStream();

    /**
     * Writes the event to the current outputStream using the OutputHandler set on
     * the StreamAdapter.
     * 
     * @param event the event to write to the stream
     * @throws IOException
     */
    void write(UMOEvent event) throws IOException;

    /**
     * The Output handler is a callback that will handle the writing to an output
     * Stream when the Stream is available
     * 
     * @return the handler used to write to the stream
     */
    OutputHandler getOutputHandler();

    /**
     * The Output handler is a callback that will handle the writing to an output
     * Stream when the Stream is available
     * 
     * @param handler the handler used to write to the stream
     */
    void setOutputHandler(OutputHandler handler);

    /**
     * The release method is called by Mule to notify this adapter that it is no
     * longer needed. This method can be used to release any resources that a custom
     * StreamAdapter may have associated with it.
     */
    void release();
}
