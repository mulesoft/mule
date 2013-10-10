/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleEvent;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The OutputHandler is a strategy class that is used to defer the writing of the message payload 
 * until there is a stream available to write it to.
 */
public interface OutputHandler
{
    /**
     * Write the event payload to the stream. Depending on the underlying transport,
     * attachements and message properties may be written to the stream here too.
     * 
     * @param event the current event
     * @param out the output stream to write to
     * @throws IOException in case of error
     */
    void write(MuleEvent event, OutputStream out) throws IOException;
}
