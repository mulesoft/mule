/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;


import static org.mule.extension.ws.internal.ConsumeOperation.MULE_SOAP_ACTION;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.cxf.message.Message;

// TODO: MULE-10783 remove
public class HttpDispatcher {

  public Response dispatch(String address, Message message) {

    String soapAction = (String) message.getExchange().get(MULE_SOAP_ACTION);

    OutputStream os = message.getContent(OutputStream.class);
    OkHttpClient client = new OkHttpClient();

    String contentType = (String) message.get(Message.CONTENT_TYPE);
    if (contentType.contains("action")) {
      // TODO: MULE-11100: MediaType.parse cannot parse a content type that carries the action element.
      contentType = contentType.substring(0, contentType.indexOf("action")).concat("\"");
    }

    MediaType mediaType = MediaType.parse(contentType);
    RequestBody body = RequestBody.create(mediaType, os.toString());
    Request.Builder request = new Request.Builder()
        .url(address)
        .post(body)
        .addHeader("cache-control", "no-cache");

    if (soapAction != null) {
      request.addHeader("SOAPAction", soapAction);
    }

    try {
      return client.newCall(request.build()).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
