/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessageException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>HttpMessageAdapter</code> Wraps an incoming Http Request making
 * the payload and heads available a standard message adapter 
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageAdapter extends AbstractMessageAdapter
{
    private Object message = null;

    public HttpMessageAdapter(Object message) throws MessageException
    {
        if (message instanceof InputStream)
        {
            setMessage((InputStream) message);
        } else if (message instanceof String) {
            ByteArrayInputStream bais = new ByteArrayInputStream(((String)message).getBytes());
            setMessage(bais);
            try
            {
                bais.close();
            } catch (IOException e) {}
        } else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return message;
    }

    public boolean isBinary()
    {
        return message instanceof byte[];
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if (isBinary())
        {
            return (byte[]) message;
        } else
        {
            return ((String) message).getBytes();
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        if (isBinary())
        {
            return new String((byte[]) message);
        } else
        {
            return (String) message;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setMessage(InputStream message) throws MessageException
    {
        try
        {
            String firstLine = HttpParser.readLine(message);
            int space1 = firstLine.indexOf(" ");
            int space2 = firstLine.indexOf(" ", space1 + 1);
            if (space1 == -1 || space2 == -1)
            {
                throw new MessageException("Http message header line is malformed: " + firstLine);
            }
            String method = firstLine.substring(0, space1);
            String request = firstLine.substring(space1 + 1, space2);
            String httpVersion = firstLine.substring(space2 + 1);

            setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
            setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, request);
            setProperty(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion);

            //Read headers from the request as set them as properties on the event
            Header[] headers = HttpParser.parseHeaders(message);
            for (int i = 0; i < headers.length; i++)
            {
                setProperty(headers[i].getName(), headers[i].getValue());
            }

            if (method.equals(HttpConstants.METHOD_GET))
            {
                this.message = request;
            } else
            {
                boolean contentLengthNotSet = getProperty(HttpConstants.HEADER_CONTENT_LENGTH, null) == null;
                int contentLength = Integer.parseInt((String)getProperty(HttpConstants.HEADER_CONTENT_LENGTH, String.valueOf(1024 * 32)));

                byte[] buffer = new byte[contentLength];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len = 0;
                int bytesWritten = 0;
                //Ensure we read all bytes, http connections may be slow
                //to send all bytes in  consistent stream.  I've only seen
                //this when using Axis...
                while (bytesWritten != contentLength)
                {
                    len = message.read(buffer);
                    if(len!=-1) {
                        baos.write(buffer, 0, len);
                        bytesWritten+=len;
                        if(contentLengthNotSet) {
                            contentLength=bytesWritten;
                        }

                    }
                }

                if (isText((String) getProperty(HttpConstants.HEADER_CONTENT_TYPE)))
                {
                    this.message = new String(baos.toByteArray());
                } else
                {
                    this.message = baos.toString();
                }
                baos.close();
            }
        } catch (Exception e)
        {
            throw new MessageException("Failed to parse Http Headers: " + e, e);
        }
    }

    protected boolean isText(String contentType)
    {
        if (contentType == null) return true;
        return (contentType.startsWith("text/"));
    }
}
