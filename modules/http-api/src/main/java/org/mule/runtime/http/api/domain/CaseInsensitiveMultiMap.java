/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.domain;

import static java.util.Collections.unmodifiableMap;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.api.util.MultiMap;

import java.util.LinkedHashMap;

/**
 * {@link MultiMap} where the key's case is not taken into account when looking for it, adding or aggregating it.
 *
 * @since 4.0
 */
@NoExtend
public class CaseInsensitiveMultiMap extends MultiMap<String, String> {

  private static final long serialVersionUID = -3754163327838153655L;

  protected final boolean optimized;

  public CaseInsensitiveMultiMap() {
    this(new MultiMap<>());
  }

  public CaseInsensitiveMultiMap(boolean optimized) {
    this(new MultiMap<>(), optimized);
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> paramsMap) {
    this(paramsMap, true);
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> paramsMap, boolean optimized) {
    this.optimized = optimized;
    this.paramsMap = optimized
        ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
        : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    putAll(paramsMap);
  }

  @Override
  public CaseInsensitiveMultiMap toImmutableMultiMap() {
    if (this instanceof ImmutableCaseInsensitiveMultiMap) {
      return this;
    }
    return new ImmutableCaseInsensitiveMultiMap(this);
  }

  private static class ImmutableCaseInsensitiveMultiMap extends CaseInsensitiveMultiMap {

    private static final long serialVersionUID = -1048913048598100657L;

    public ImmutableCaseInsensitiveMultiMap(CaseInsensitiveMultiMap caseInsensitiveMultiMap) {
      super(caseInsensitiveMultiMap, caseInsensitiveMultiMap.optimized);
      this.paramsMap = unmodifiableMap(paramsMap);
    }
  }
}
