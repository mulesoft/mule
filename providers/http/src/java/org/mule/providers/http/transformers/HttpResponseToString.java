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
 */

package org.mule.providers.http.transformers;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpResponse;
import org.mule.providers.http.ResponseWriter;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * Converts an Http Response object to String. Not that the response headers are
 * not preserved.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpResponseToString extends AbstractTransformer
{

    public HttpResponseToString()
    {
        registerSourceType(HttpResponse.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
            HttpResponse response = (HttpResponse)src;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
            OutputStream outstream = bos;
            ResponseWriter writer = new ResponseWriter(outstream, encoding);
            writer.println(response.getStatusLine());
            Iterator item = response.getHeaderIterator();
            while (item.hasNext()) {
                Header header = (Header)item.next();
                writer.print(header.toExternalForm());
            }
            writer.println();
            writer.flush();

            InputStream content = response.getBody();
            if (content != null) {
                Header transferenc = response.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
                if (transferenc != null) {
                    response.removeHeaders(HttpConstants.HEADER_CONTENT_LENGTH);
                    if (transferenc.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1) {
                        outstream = new ChunkedOutputStream(outstream);
                    }

                    IOUtils.copy(content, outstream);

                    if (outstream instanceof ChunkedOutputStream) {
                        ((ChunkedOutputStream)outstream).finish();
                    }
                }
            }

            outstream.flush();
            bos.flush();
            byte[] result = bos.toByteArray();
            outstream.close();
            writer.close();
            bos.close();
            if (getReturnClass().equals(String.class)) {
                return new String(result, encoding);
            }
            else {
                return result;
            }
        }
        catch (IOException e) {
            throw new TransformerException(this, e);
        }
    }
}
