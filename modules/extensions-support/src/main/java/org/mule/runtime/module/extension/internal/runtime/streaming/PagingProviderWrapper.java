/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

/**
 * This implementation of {@link PagingProvider} takes care of enforcing some basic behaviour of the delegate contract so that
 * users don't have to. Concerns such as logging, auto closing the delegate if the consumer has been fully consumed are addressed
 * here
 *
 * @param <C> connection type expected to handle the operations.
 * @param <T> the type of the elements in the returned pages.
 * @since 3.5.0
 */
final class PagingProviderWrapper<C, T> implements PagingProvider<C, T> {

  private static final Logger LOGGER = getLogger(PagingProviderWrapper.class);

  private final PagingProvider<C, T> delegate;
  private final ClassLoader extensionClassLoader;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public PagingProviderWrapper(PagingProvider<C, T> delegate, ExtensionModel extensionModel) {
    this.delegate = delegate;
    extensionClassLoader = getClassLoader(extensionModel);
  }

  /**
   * {@inheritDoc} Sets the closed flag to true and then delegates into the wrapped instance
   */
  @Override
  public void close(C connection) throws MuleException {
    if (closed.compareAndSet(false, true)) {
      delegate.close(connection);
    }
  }

  private void handleCloseException(Throwable t) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("Exception was found trying to close paging delegate. Execution will continue", t);
    }
  }

  /**
   * {@inheritDoc} This implementation already takes care of returning <code>null</code> if the delegate is closed or if the
   * obtained page is <code>null</code> or empty. It delegates into the wrapped instance to actually obtain the page.
   */
  @Override
  public List<T> getPage(C connection) {
    if (closed.get()) {
      LOGGER.debug("paging delegate is closed. Returning null");
      return null;
    }

    Thread currentThread = currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    setContextClassLoader(currentThread, currentClassLoader, extensionClassLoader);
    try {
      List<T> page = delegate.getPage(connection);
      if (isEmpty(page)) {
        try {
          LOGGER.debug("Empty page was obtained. Closing delegate since this means that the data source has been consumed");
          close(connection);
        } catch (Exception e) {
          handleCloseException(e);
        }
      }
      return page;
    } finally {
      setContextClassLoader(currentThread, extensionClassLoader, currentClassLoader);
    }
  }

  @Override
  public Optional<Integer> getTotalResults(C connection) {
    Optional<Integer> size = delegate.getTotalResults(connection);
    return size != null ? size : empty();
  }

  @Override
  public boolean useStickyConnections() {
    return delegate.useStickyConnections();
  }
}
