/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

public class DrStrangeOperations {

  @Inject
  private MuleContext muleContext;

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
}
