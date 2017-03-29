/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DrStrangeOperations {

  public String seekStream(@UseConfig DrStrange dr, @Optional(defaultValue = PAYLOAD) InputStream stream, int position)
      throws IOException {
    checkArgument(stream instanceof CursorStream, "Stream was not cursored");

    CursorStream cursor = (CursorStream) stream;
    cursor.seek(position);

    return readStream(dr, cursor);
  }

  public String readStream(@UseConfig DrStrange dr, @Optional(defaultValue = PAYLOAD) InputStream stream) throws IOException {
    return IOUtils.toString(stream);
  }

  public InputStream toStream(@UseConfig DrStrange dr, @Optional(defaultValue = PAYLOAD) String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  public void crashCar(@UseConfig DrStrange dr) {
    throw new RuntimeException();
  }

  public List<String> readObjectStream(@Content Iterator<String> values) {
    List<String> objects = new LinkedList<>();
    while (values.hasNext()) {
      objects.add(values.next());
    }

    return objects;
  }

  public PagingProvider<MysticConnection, String> sayMagicWords(@Content List<String> values,
                                                                int fetchSize,
                                                                @Connection MysticConnection connection) {
    final AtomicInteger index = new AtomicInteger(0);

    return new PagingProvider<MysticConnection, String>() {

      @Override
      public List<String> getPage(MysticConnection connection) {
        final int i = index.get();
        if (i >= values.size()) {
          return emptyList();
        }

        List<String> words = values.subList(i, i + fetchSize);
        index.addAndGet(fetchSize);

        return words;
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(MysticConnection connection) {
        return of(values.size());
      }

      @Override
      public void close() throws IOException {

      }
    };
  }

}
