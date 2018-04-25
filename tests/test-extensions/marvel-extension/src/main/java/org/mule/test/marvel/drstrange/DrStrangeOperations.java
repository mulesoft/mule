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
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition.CUSTOM_ERROR;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.reference.FlowReference;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

public class DrStrangeOperations {

  @Inject
  private ConfigurationComponentLocator locator;

  @MediaType(TEXT_PLAIN)
  public String seekStream(@Connection MysticConnection connection, @Optional(defaultValue = PAYLOAD) InputStream stream,
                           int position)
      throws IOException {
    checkArgument(stream instanceof CursorStream, "Stream was not cursored");

    CursorStream cursor = (CursorStream) stream;
    cursor.seek(position);

    return readStream(connection, cursor);
  }

  @Throws(CustomErrorProvider.class)
  @MediaType(TEXT_PLAIN)
  public String readStream(@Connection MysticConnection connection, @Optional(defaultValue = PAYLOAD) InputStream stream)
      throws IOException {
    try {
      return IOUtils.toString(stream);
    } catch (Exception e) {
      throw new CustomErrorException(e, CUSTOM_ERROR);
    }
  }

  @MediaType(TEXT_PLAIN)
  public InputStream toStream(@Connection MysticConnection connection, @Optional(defaultValue = PAYLOAD) String data) {
    return connection.manage(new ByteArrayInputStream(data.getBytes()));
  }

  public void crashCar(@Config DrStrange dr) {
    throw new RuntimeException();
  }

  @Stereotype(ReferableOperationStereotypeDefinition.class)
  public void withFlowReference(@Config DrStrange dr, @Optional @FlowReference String flowRef, String name) {

    if (!StringUtils.isBlank(flowRef)) {
      if (!locator.find(Location.builder().globalName(flowRef).build()).isPresent()) {
        throw new IllegalArgumentException("The referenced flow does not exist in this application");
      }
    }

  }

  public List<String> readObjectStream(@Content Iterator<String> iteratorValues) {
    List<String> objects = new LinkedList<>();
    while (iteratorValues.hasNext()) {
      objects.add(iteratorValues.next());
    }

    return objects;
  }

  public PagingProvider<MysticConnection, String> sayMagicWords(@Content List<String> values,
                                                                int fetchSize) {
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
      public void close(MysticConnection connection) throws MuleException {

      }
    };
  }
}
