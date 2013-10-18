/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import org.mule.RequestContext;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;
import org.mule.transport.http.ResponseWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Converts an Http Response object to String. Note that the response headers are
 * preserved.
 */
public class HttpResponseToString extends AbstractTransformer
{

    public HttpResponseToString()
    {
        registerSourceType(DataTypeFactory.create(HttpResponse.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    /**
     * Perform the transformation to always return a String object
     */
    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            HttpResponse response = (HttpResponse)src;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
            OutputStream outstream = bos;
            ResponseWriter writer = new ResponseWriter(outstream, encoding);
            writer.println(response.getStatusLine());
            Iterator item = response.getHeaderIterator();
            while (item.hasNext())
            {
                Header header = (Header)item.next();
                writer.print(header.toExternalForm());
            }
            writer.println();
            writer.flush();

            if (response.hasBody())
            {
                OutputHandler handler = response.getBody();
                Header transferenc = response.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
                if (transferenc != null)
                {
                    response.removeHeaders(HttpConstants.HEADER_CONTENT_LENGTH);
                    if (transferenc.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1)
                    {
                        outstream = new ChunkedOutputStream(outstream);
                    }
                }

                handler.write(RequestContext.getEvent(), outstream);

                if (outstream instanceof ChunkedOutputStream)
                {
                    ((ChunkedOutputStream)outstream).finish();
                }
            }

            outstream.flush();
            bos.flush();
            byte[] result = bos.toByteArray();
            outstream.close();
            writer.close();
            bos.close();

            String output = null;
            try
            {
                output = new String(result, encoding);
            }
            catch (UnsupportedEncodingException uee)
            {
                // I believe this is never reached since a TransformerExcpetion
                // is thrown before at new ResponseWriter(outstream, encoding) if
                // encoding is not supported
                output = new String(result);
            }

            return output;
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
    }
}
