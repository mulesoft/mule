/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MysticConnection {

  private final List<InputStream> streams = new CopyOnWriteArrayList<>();

  public InputStream manage(InputStream inputStream) {
    final InputStreamWrapper managed = new InputStreamWrapper(inputStream);
    streams.add(managed);

    return managed;
  }

  public void close() {
    streams.forEach(IOUtils::closeQuietly);
  }

  private class InputStreamWrapper extends InputStream {

    private final InputStream delegate;
    private boolean closed = false;

    public InputStreamWrapper(InputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      if (closed) {
        return -1;
      }
      return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      if (closed) {
        return -1;
      }
      return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (closed) {
        return -1;
      }
      return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
      return delegate.available();
    }

    @Override
    public void close() throws IOException {
      closed = true;
      delegate.close();
    }

    @Override
    public void mark(int readlimit) {
      delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
      delegate.reset();
    }

    @Override
    public boolean markSupported() {
      return delegate.markSupported();
    }
  }
}
