/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.tcp.TcpProtocol;

import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * The DefaultProtocol class is an application level tcp protocol that does
 * nothing. Reading is performed in reading the socket until no more bytes are
 * available. Writing simply writes the data to the socket.
 *
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class DefaultProtocol implements TcpProtocol
{

    private static final int BUFFER_SIZE = 8192;

    private static final Log logger = LogFactory.getLog(DefaultProtocol.class);

    public byte[] read(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);

        byte[] buffer = new byte[BUFFER_SIZE];
        int len = 0;
        try {
            while ((len = is.read(buffer)) == 0) {
                // wait
            }
        } catch (SocketException e) {
            // do not pollute the log with a stacktrace, log only the message
            logger.debug("Socket exception occured: " + e.getMessage());
            return null;
        } catch (SocketTimeoutException e) {
            logger.debug("Socket timeout, returning null.");
            return null;
        }
        if (len == -1) {
            return null;
        } else {
            do {
                baos.write(buffer, 0, len);
                if (len < buffer.length) {
                    break;
                }
                int av = is.available();
                if (av == 0) {
                    break;
                }
            } while ((len = is.read(buffer)) > 0);

            baos.flush();
            baos.close();
            return baos.toByteArray();
        }
    }

    public void write(OutputStream os, byte[] data) throws IOException
    {
        os.write(data);
    }

}
