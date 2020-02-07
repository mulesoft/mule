/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.domain;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.http.api.server.HttpServerProperties.PRESERVE_HEADER_CASE;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.api.util.MultiMap;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * {@link MultiMap} where the key's case is not taken into account when looking for it, adding or aggregating it.
 *
 * @since 4.0
 */
@NoExtend
public class CaseInsensitiveMultiMap extends AbstractCaseInsensitiveMultiMap {

  private static final long serialVersionUID = -3754163327838153655L;

  private static final CaseInsensitiveMultiMap EMPTY_MAP = new CaseInsensitiveMultiMap().toImmutableMultiMap();

  /**
   * Returns an empty case-insensitive-multi-map (immutable). This map is serializable.
   *
   * <p>
   * This example illustrates the type-safe way to obtain an empty map:
   *
   * <pre>
   *
   * CaseInsensitiveMultiMap s = CaseInsensitiveMultiMap.emptyCaseInsensitiveMultiMap();
   * </pre>
   *
   * @return an empty case-insensitive-multi-map
   * @since 1.3
   */
  public static CaseInsensitiveMultiMap emptyCaseInsensitiveMultiMap() {
    return EMPTY_MAP;
  }

  protected final boolean optimized;

  public CaseInsensitiveMultiMap() {
    this(!PRESERVE_HEADER_CASE);
  }

  public CaseInsensitiveMultiMap(boolean optimized) {
    this.optimized = optimized;
    this.paramsMap = optimized
        ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
        : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> paramsMap) {
    this(paramsMap, !PRESERVE_HEADER_CASE);
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> multiMap, boolean optimized) {
    /* this.optimized = optimized;
    this.paramsMap = optimized
        ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
        : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    putAll(multiMap);*/
    this.optimized = optimized;
    if (multiMap instanceof CaseInsensitiveMultiMap) {
      //this.paramsMap = ((CaseInsensitiveMapWrapper)((CaseInsensitiveMultiMap)multiMap).paramsMap).copy();
      this.createParamsMap((CaseInsensitiveMultiMap) multiMap);
    } else {
      this.paramsMap = optimized
          ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
          : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
      putAll(multiMap);
    }
  }

  private void createParamsMap(CaseInsensitiveMultiMap multiMap) {
    if (optimized) {

      if (multiMap.paramsMap instanceof OptimizedCaseInsensitiveMapWrapper) {
        this.paramsMap = ((OptimizedCaseInsensitiveMapWrapper<LinkedList<String>>) multiMap.paramsMap).copy();
      } else {
        new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
        if (!multiMap.isEmpty()) {
          putAll(multiMap);
        }
      }

    } else {

      if (multiMap.paramsMap instanceof CaseInsensitiveMapWrapper) {
        this.paramsMap = ((CaseInsensitiveMapWrapper<LinkedList<String>>) multiMap.paramsMap).copy();
      } else {
        new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
        if (!multiMap.isEmpty()) {
          putAll(multiMap);
        }
      }

    }
  }

  public static AbstractCaseInsensitiveMultiMap fromMultiMap(MultiMap<String, String> multiMap) {
    if (multiMap != null) {
      if (multiMap instanceof AbstractCaseInsensitiveMultiMap) {
        return (AbstractCaseInsensitiveMultiMap) multiMap;
      } else {
        return new CaseInsensitiveMultiMap(multiMap);
      }
    } else {
      return new CaseInsensitiveMultiMap();
    }
  }

  public static MultiMap<String, String> toMap(CaseInsensitiveMultiMap multiMap) {
    return new MultiMap<>(multiMap);
  }

  @Override
  public CaseInsensitiveMultiMap toImmutableMultiMap() {
    if (this.isEmpty() && EMPTY_MAP != null) {
      return emptyCaseInsensitiveMultiMap();
    }

    return new ImmutableCaseInsensitiveMultiMap(this);
  }

  private static class ImmutableCaseInsensitiveMultiMap extends CaseInsensitiveMultiMap {

    private static final long serialVersionUID = 3979199753090570561L;

    public ImmutableCaseInsensitiveMultiMap(CaseInsensitiveMultiMap caseInsensitiveMultiMap) {
      super(caseInsensitiveMultiMap, caseInsensitiveMultiMap.optimized);
      this.paramsMap = unmodifiableMap(paramsMap);
    }

    @Override
    public CaseInsensitiveMultiMap toImmutableMultiMap() {
      return this;
    }
  }

  public static AbstractCaseInsensitiveMultiMap unmodifiableCaseInsensitiveMultiMap(AbstractCaseInsensitiveMultiMap m) {
    requireNonNull(m);
    if (m instanceof UnmodifiableCaseInsensitiveMultiMap || m instanceof ImmutableCaseInsensitiveMultiMap) {
      return m;
    } else if (m instanceof CaseInsensitiveMultiMap) {
      return new UnmodifiableCaseInsensitiveMultiMap((CaseInsensitiveMultiMap) m);
    }
    return null;
  }
}
