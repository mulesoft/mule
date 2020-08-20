/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Implementations provide the functionality to decorate {@link Cursor}s in order to capture {@link PayloadStatistics} from it.
 *
 * @since 4.4, 4.3.1
 */
public interface CursorComponentDecoratorFactory {

  /**
   * If statistics are enabled, decorates the provided {@link Iterator} for counting the iterated objects.
   * <p>
   * Ref: {@link PayloadStatistics#getInputObjectCount()}.
   *
   * @param decorated the {@link Iterator} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *        measured volume to a specific execution.
   * @return the decorated {@link Iterator}.
   */
  <T> Iterator<T> decorateInput(Iterator<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link InputStream} for counting the streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getInputByteCount()}.
   *
   * @param decorated the {@link InputStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *        measured volume to a specific execution.
   * @return the decorated {@link InputStream}.
   */
  InputStream decorateInput(InputStream decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link PagingProvider} for counting the received objects.
   * <p>
   * When the result of an operation is obtained through a {@link PagingProvider}, the statistics must account for the load of the
   * page, rather than each individual object being fetched.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated the {@link PagingProvider} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *        measured volume to a specific execution.
   * @return the decorated {@link PagingProvider}.
   */
  <C, T> PagingProvider<C, T> decorateOutput(PagingProvider<C, T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Iterator} for counting the iterated objects.
   * <p>
   * This method must only be used for iterators that are not backed by a {@link PagingProvider}. In that case, use
   * {@link #decorateOutput(PagingProvider)} instead.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated the {@link Iterator} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *        measured volume to a specific execution.
   * @return the decorated {@link Iterator}.
   */
  <T> Iterator<T> decorateOutput(Iterator<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link InputStream} for counting the streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputByteCount()}.
   *
   * @param decorated the {@link InputStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *        measured volume to a specific execution.
   * @return the decorated {@link InputStream}.
   */
  InputStream decorateOutput(InputStream decorated, String correlationId);

}
