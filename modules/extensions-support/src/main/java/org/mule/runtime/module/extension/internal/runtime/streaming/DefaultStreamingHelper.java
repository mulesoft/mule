/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Default implementation of {@link StreamingHelper}. A new instance should be use per each component execution.
 *
 * @see StreamingHelper
 * @since 4.0
 */
public class DefaultStreamingHelper implements StreamingHelper {

  private final CursorProviderFactory cursorProviderFactory;
  private final StreamingManager streamingManager;
  private final CoreEvent event;

  /**
   * Creates a new instance
   *
   * @param cursorProviderFactory the {@link CursorProviderFactory} configured on the executing component
   * @param streamingManager      the application's {@link StreamingManager}
   * @param event                 the {@link CoreEvent} being currently executed
   */
  public DefaultStreamingHelper(CursorProviderFactory cursorProviderFactory,
                                StreamingManager streamingManager,
                                CoreEvent event) {
    this.cursorProviderFactory = cursorProviderFactory;
    this.streamingManager = streamingManager;
    this.event = event;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <K> Map<K, Object> resolveCursors(Map<K, Object> map, boolean recursive) {
    return resolveMap(map, recursive, ResolverUtils::resolveCursor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <K> Map<K, Object> resolveCursorProviders(Map<K, Object> map, boolean recursive) {
    return resolveMap(map, recursive, this::resolveCursorProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object resolveCursor(Object value) {
    return ResolverUtils.resolveCursor(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object resolveCursorProvider(Object value) {
    if (value instanceof Cursor) {
      return ((Cursor) value).getProvider();
    }

    if (value instanceof InputStream) {
      value = resolveCursorStreamProvider((InputStream) value);
    } else if (value instanceof StreamingIterator) {
      value = resolveCursorIteratorProvider((StreamingIterator) value);
    }

    return value;
  }

  private Object resolveCursorStreamProvider(InputStream value) {
    CursorProviderFactory factory = cursorProviderFactory.accepts(value)
        ? cursorProviderFactory
        : streamingManager.forBytes().getDefaultCursorProviderFactory();

    return factory.of(event, value);
  }

  private Object resolveCursorIteratorProvider(Iterator value) {
    CursorProviderFactory factory = cursorProviderFactory.accepts(value)
        ? cursorProviderFactory
        : streamingManager.forObjects().getDefaultCursorProviderFactory();

    return factory.of(event, value);
  }

  private <K> Map<K, Object> resolveMap(Map<K, Object> map, boolean recursive, Function<Object, Object> valueMapper) {
    checkArgument(map != null, "Map cannot be null");
    Map<K, Object> resolved;
    try {
      resolved = ClassUtils.instantiateClass(map.getClass());
    } catch (Exception e) {
      resolved = new LinkedHashMap<>();
    }

    for (Map.Entry<K, Object> entry : map.entrySet()) {
      Object value = valueMapper.apply(entry.getValue());

      if (recursive && value instanceof Map) {
        value = resolveCursors((Map) value, recursive);
      }

      resolved.put(entry.getKey(), value);
    }

    return resolved;
  }
}
