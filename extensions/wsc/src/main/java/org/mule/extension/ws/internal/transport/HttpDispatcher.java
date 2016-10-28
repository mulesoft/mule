/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;


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

    OutputStream os = message.getContent(OutputStream.class);
    OkHttpClient client = new OkHttpClient();
    MediaType mediaType = MediaType.parse((String) message.get(Message.CONTENT_TYPE));
    RequestBody body = RequestBody.create(mediaType, os.toString());
    Request request = new Request.Builder()
        .url(address)
        .post(body)
        .addHeader("cache-control", "no-cache")
        .build();

    try {
      return client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
