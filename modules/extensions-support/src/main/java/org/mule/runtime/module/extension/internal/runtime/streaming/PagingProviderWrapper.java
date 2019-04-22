/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static java.util.Optional.empty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
  private final ExtensionModel extensionModel;
  private boolean closed = false;

  public PagingProviderWrapper(PagingProvider<C, T> delegate, ExtensionModel extensionModel) {
    this.delegate = delegate;
    this.extensionModel = extensionModel;
  }

  /**
   * {@inheritDoc} Sets the closed flag to true and then delegates into the wrapped instance
   */
  @Override
  public void close(C connection) throws MuleException {
    closed = true;
    delegate.close(connection);
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
    if (closed) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("paging delegate is closed. Returning null");
      }
      return null;
    }

    List<T> page = withContextClassLoader(MuleExtensionUtils.getClassLoader(extensionModel), () -> {
      List<T> delegatePage = delegate.getPage(connection);
      if (isEmpty(delegatePage)) {
        try {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Empty page was obtained. Closing delegate since this means that the data source has been consumed");
          }
          close(connection);
        } catch (Exception e) {
          handleCloseException(e);
        }
      }
      return delegatePage;
    });

    return page;
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
