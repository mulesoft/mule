/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpResponse;
import org.mule.compatibility.transport.http.ResponseWriter;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Converts an Http Response object to String. Note that the response headers are preserved.
 */
public class HttpResponseToString extends AbstractTransformer {

  public HttpResponseToString() {
    registerSourceType(DataType.fromType(HttpResponse.class));
    setReturnDataType(DataType.STRING);
  }

  /**
   * Perform the transformation to always return a String object
   */
  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    try {
      HttpResponse response = (HttpResponse) src;
      ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
      OutputStream outstream = bos;
      ResponseWriter writer = new ResponseWriter(outstream, encoding);
      writer.println(response.getStatusLine());
      Iterator item = response.getHeaderIterator();
      while (item.hasNext()) {
        Header header = (Header) item.next();
        writer.print(header.toExternalForm());
      }
      writer.println();
      writer.flush();

      if (response.hasBody()) {
        OutputHandler handler = response.getBody();
        Header transferenc = response.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
        if (transferenc != null) {
          response.removeHeaders(HttpConstants.HEADER_CONTENT_LENGTH);
          if (transferenc.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1) {
            outstream = new ChunkedOutputStream(outstream);
          }
        }

        handler.write(RequestContext.getEvent(), outstream);

        if (outstream instanceof ChunkedOutputStream) {
          ((ChunkedOutputStream) outstream).finish();
        }
      }

      outstream.flush();
      bos.flush();
      byte[] result = bos.toByteArray();
      outstream.close();
      writer.close();
      bos.close();

      return new String(result, encoding);
    } catch (IOException e) {
      throw new TransformerException(this, e);
    }
  }
}
