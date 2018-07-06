/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.repeat;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.io.IOException;
import java.io.InputStream;

public class StreamProviderProcessor extends AbstractComponent implements Processor {

  public static final int END = -1;
  public static final String SIXTEEN_BYTES = "1234567890-=+*^,";
  public static final String BYTES_PART = repeat(SIXTEEN_BYTES, 8);
  public static final byte[] BYTES = repeat(BYTES_PART, 8).getBytes(UTF_8);

  private int size;

  public StreamProviderProcessor(int size) {
    this.size = size;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return CoreEvent.builder(event).message(Message.of(new SizedStream(size))).build();
  }

  private class SizedStream extends InputStream {

    private int read;
    private int size;

    public SizedStream(int size) {
      this.read = 0;
      this.size = size;
    }

    @Override
    public int read() throws IOException {
      if (read == size) {
        return END;
      } else {
        read++;
        return 42;
      }
    }

    @Override
    public int read(byte[] b) throws IOException {
      if (read == size) {
        return END;
      } else {
        int toRead = min(size - read, min(b.length, BYTES.length));
        read += toRead;
        System.arraycopy(BYTES, 0, b, 0, toRead);
        return toRead;
      }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (read == size) {
        return END;
      } else {
        int toRead = min(size - read, min(len, min(b.length, BYTES.length)));
        read += toRead;
        System.arraycopy(BYTES, 0, b, off, toRead);
        return toRead;
      }
    }
  }
}
