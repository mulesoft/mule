/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The TcpProtocol interface enables to plug different application level
 * protocols on a TcpConnector.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public interface TcpProtocol
{

    /**
     * Reads the input stream and returns a whole message.
     * 
     * @param is the input stream
     * @return an array of byte containing a full message
     * @throws IOException if an exception occurs
     */
    byte[] read(InputStream is) throws IOException;

    /**
     * Write the specified message to the output stream.
     * 
     * @param os the output stream to write to
     * @param data the data to write
     * @throws IOException if an exception occurs
     */
    void write(OutputStream os, byte[] data) throws IOException;
}
