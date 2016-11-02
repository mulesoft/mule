/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOGGER = LoggerFactory.getLogger(PagingProviderWrapper.class);

  private final PagingProvider<C, T> wrapped;
  private boolean closed = false;

  public PagingProviderWrapper(PagingProvider<C, T> wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * {@inheritDoc} Sets the closed flag to true and then delegates into the wrapped instance
   */
  @Override
  public void close() throws IOException {
    this.closed = true;
    this.wrapped.close();
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
    if (this.closed) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("paging delegate is closed. Returning null");
      }
      return null;
    }

    List<T> page = this.wrapped.getPage(connection);
    if (isEmpty(page)) {
      try {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Empty page was obtained. Closing delegate since this means that the data source has been consumed");
        }

        this.close();
      } catch (Exception e) {
        this.handleCloseException(e);
      }
    }

    return page;
  }

  @Override
  public Optional<Integer> getTotalResults(C connection) {
    return this.wrapped.getTotalResults(connection);
  }
}
