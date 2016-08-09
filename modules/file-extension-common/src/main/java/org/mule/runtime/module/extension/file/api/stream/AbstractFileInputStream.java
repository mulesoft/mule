/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.stream;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.module.extension.file.api.FileSystem;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

/**
 * Base class for {@link InputStream} instances returned by connectors which operate over a {@link FileSystem}.
 * <p>
 * It's an {@link AutoCloseInputStream} which also contains the concept of a {@link PathLock} which is released when the stream is
 * closed or fully consumed.
 * <p>
 * Because in most implementations the actual reading of the stream requires initialising/maintaining a connection, instances are
 * created through a {@link LazyStreamSupplier}. This allows such connection/resource to be provisioned lazily. This is very
 * useful in cases such as {@link FileSystem#list(String, boolean, MuleMessage, Predicate)}. Being able to only lazily establish
 * the connections, prevents the connector from opening many connections at the same time, at the risk that many of them might end
 * up not being necessary at the same place.
 *
 * @since 4.0
 */
public abstract class AbstractFileInputStream extends AutoCloseInputStream {

  private static InputStream createLazyStream(LazyStreamSupplier streamFactory) {
    return (InputStream) Enhancer.create(InputStream.class,
                                         (MethodInterceptor) (proxy, method, arguments, methodProxy) -> methodProxy
                                             .invoke(streamFactory.get(), arguments));
  }

  private final LazyStreamSupplier streamSupplier;
  private final PathLock lock;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public AbstractFileInputStream(LazyStreamSupplier streamSupplier, PathLock lock) {
    super(createLazyStream(streamSupplier));
    this.lock = lock;
    this.streamSupplier = streamSupplier;
  }

  /**
   * Closes the stream and invokes {@link PathLock#release()} on the {@link #lock}.
   * <p>
   * Because the actual stream is lazily opened, the possibility exists for this method being invoked before the
   * {@link #streamSupplier} is used. In such case, this method will not fail.
   *
   * @throws IOException in case of error
   */
  @Override
  public final synchronized void close() throws IOException {
    try {
      if (closed.compareAndSet(false, true) && streamSupplier.isSupplied()) {
        doClose();
      }
    } finally {
      lock.release();
    }
  }

  protected void doClose() throws IOException {
    super.close();
  }

  public boolean isLocked() {
    return lock.isLocked();
  }
}
