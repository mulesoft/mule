/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FtpMessageDispatcher extends AbstractMessageDispatcher
{

    protected FtpConnector connector;

    public FtpMessageDispatcher(FtpConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    public void doDispose()
    {
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        FTPClient client = null;
        UMOEndpointURI uri = event.getEndpoint().getEndpointURI();
        try {
            String filename = (String) event.getProperty(FtpConnector.PROPERTY_FILENAME);

            if (filename == null) {
                String outPattern = (String) event.getProperty(FtpConnector.PROPERTY_OUTPUT_PATTERN);
                if (outPattern == null) {
                    outPattern = connector.getOutputPattern();
                }
                filename = generateFilename(event, outPattern);
            }
            if (filename == null) {
                throw new IOException("Filename is null");
            }

            byte[] buf;
            Object data = event.getTransformedMessage();
            if (data instanceof byte[]) {
                buf = (byte[]) data;
            } else {
                buf = data.toString().getBytes();
            }

            client = connector.getFtp(uri);
            if (!client.changeWorkingDirectory(uri.getPath())) {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }
            if (!client.storeFile(filename, new ByteArrayInputStream(buf))) {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }

        } finally {
            connector.releaseFtp(uri, client);
        }
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getDelegateSession() throws UMOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    private String generateFilename(UMOEvent event, String pattern)
    {
        if (pattern == null) {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(event.getMessage(), pattern);
    }

}
