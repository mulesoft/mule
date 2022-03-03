/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.sdk.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * Implementations provide the functionality to decorate {@link Cursor}s in order to capture {@link PayloadStatistics} from it.
 *
 * @since 4.4, 4.3.1
 * @deprecated since 4.4.1, 4.5.0. Payload statistics is no longer supported.
 */
@Deprecated
public interface CursorComponentDecoratorFactory {

  /**
   * If statistics are enabled, increment the invocation count.
   * <p>
   * Ref: {@link PayloadStatistics#incrementInvocationCount()}.
   *
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   */
  void incrementInvocationCount(String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Collection} for counting the read objects.
   * <p>
   * Ref: {@link PayloadStatistics#getInputObjectCount()}.
   *
   * @param decorated     the {@link Collection} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link Collection}.
   */
  <T> Collection<T> decorateInput(Collection<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Iterator} for counting the iterated objects.
   * <p>
   * Ref: {@link PayloadStatistics#getInputObjectCount()}.
   *
   * @param decorated     the {@link Iterator} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link Iterator}.
   */
  <T> Iterator<T> decorateInput(Iterator<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link InputStream} for counting the streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getInputByteCount()}.
   *
   * @param decorated     the {@link InputStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link InputStream}.
   */
  InputStream decorateInput(InputStream decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link CursorStream}.
   * <p>
   * Ref: {@link PayloadStatistics#getInputByteCount()}.
   *
   * @param decorated     the {@link CursorStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link CursorStream}.
   */
  CursorStream decorateInput(CursorStream decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link PagingProvider} for counting the received objects.
   * <p>
   * When the result of an operation is obtained through a {@link PagingProvider}, the statistics must account for the load of the
   * page, rather than each individual object being fetched.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated     the {@link PagingProvider} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link PagingProvider}.
   */
  <C, T> PagingProvider<C, T> decorateOutput(PagingProvider<C, T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Iterator} for counting the iterated objects.
   * <p>
   * This method must only be used for iterators that are not backed by a {@link PagingProvider}. In that case, use
   * {@link #decorateOutput(PagingProvider, String)} instead.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated     the {@link Iterator} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link Iterator}.
   */
  <T> Iterator<T> decorateOutput(Iterator<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link InputStream} for counting the streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputByteCount()}.
   *
   * @param decorated     the {@link InputStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link InputStream}.
   */
  InputStream decorateOutput(InputStream decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link CursorStream} for counting the streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputByteCount()}.
   *
   * @param decorated     the {@link CursorStream} to decorate.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link CursorStream}.
   */
  CursorStream decorateOutput(CursorStream decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Collection} for counting its objects and, if applicable, its
   * elements for counting its objects or streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputByteCount()}, {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated     the {@link Collection} to decorate along with its elements.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link Collection} with the decorated elements.
   */
  <T> Collection<T> decorateOutputCollection(Collection<T> decorated, String correlationId);

  /**
   * If statistics are enabled, decorates the provided {@link Iterator} for counting its objects and, if applicable, its elements
   * for counting its objects or streamed bytes.
   * <p>
   * Ref: {@link PayloadStatistics#getOutputByteCount()}, {@link PayloadStatistics#getOutputObjectCount()}.
   *
   * @param decorated     the {@link Iterator} to decorate along with its elements.
   * @param correlationId information to be used in the case a detailed report needs to be obtained, allowing to match the
   *                      measured volume to a specific execution.
   * @return the decorated {@link Iterator} with the decorated elements.
   */
  <T> Iterator<T> decorateOutputIterator(Iterator<T> decorated, String correlationId);

  /**
   * If statistics are enabled, counts the received bytes.
   * 
   * @param v the content byte array.
   */
  void computeInputByteCount(byte[] v);
}
