/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.streaming;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.privileged.util.EventUtils.getRoot;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
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

  private final CursorStreamProviderFactory cursorStreamProviderFactory;
  private final CursorIteratorProviderFactory cursorIteratorProviderFactory;
  private final CoreEvent event;
  private final ComponentLocation originatingLocation;

  /**
   * Creates a new instance
   *
   * @param cursorStreamProviderFactory   the {@link CursorStreamProviderFactory} to be used for byte streaming
   * @param cursorIteratorProviderFactory the {@link CursorIteratorProviderFactory} to be used for object streaming
   * @param event                         the {@link CoreEvent} being currently executed
   */
  public DefaultStreamingHelper(CursorStreamProviderFactory cursorStreamProviderFactory,
                                CursorIteratorProviderFactory cursorIteratorProviderFactory,
                                CoreEvent event,
                                ComponentLocation originatingLocation) {
    this.cursorStreamProviderFactory = cursorStreamProviderFactory;
    this.cursorIteratorProviderFactory = cursorIteratorProviderFactory;
    this.event = event;
    this.originatingLocation = originatingLocation;
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
    } else if (value instanceof Iterator) {
      value = resolveCursorIteratorProvider((Iterator) value);
    } else if (value instanceof TypedValue) {
      value = resolveCursorTypedValueProvider((TypedValue) value);
    }

    return value;
  }

  private Object resolveCursorStreamProvider(InputStream value) {
    return cursorStreamProviderFactory.of(getRoot(event.getContext()), value, originatingLocation);
  }

  private Object resolveCursorIteratorProvider(Iterator value) {
    return cursorIteratorProviderFactory.of(getRoot(event.getContext()), value, originatingLocation);
  }

  private TypedValue resolveCursorTypedValueProvider(TypedValue value) {
    Object resolvedValue = resolveCursorProvider(value.getValue());
    if (resolvedValue != value.getValue()) {
      return new TypedValue(resolvedValue, value.getDataType());
    }
    return value;
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
