/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.domain;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.http.api.server.HttpServerProperties.PRESERVE_HEADER_CASE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.el.DataTypeAware;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.api.util.MultiMap;
import org.slf4j.Logger;

import java.util.LinkedHashMap;

/**
 * {@link MultiMap} where the key's case is not taken into account when looking for it, adding or aggregating it.
 *
 * @since 4.0
 */
@NoExtend
public class CaseInsensitiveMultiMap extends MultiMap<String, String> implements DataTypeAware {

  private static final Logger LOGGER = getLogger(CaseInsensitiveMultiMap.class);

  private static final long serialVersionUID = -3754163327838153655L;

  private static final CaseInsensitiveMultiMap EMPTY_MAP = new CaseInsensitiveMultiMap().toImmutableMultiMap();

  private static final DataType dataType = DataType.builder()
      .mapType(CaseInsensitiveMultiMap.class)
      .keyType(String.class)
      .valueType(String.class)
      .build();

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
    LOGGER.error("Constructor without parameters, line 58");
  }

  public CaseInsensitiveMultiMap(boolean optimized) {
    LOGGER.error("Constructor with boolean optimized, line 62");
    this.optimized = optimized;
    this.paramsMap = optimized
        ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
        : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> paramsMap) {
    this(paramsMap, !PRESERVE_HEADER_CASE);
    LOGGER.error("Constructor with Multimap paramsMap, line 71");
  }

  public CaseInsensitiveMultiMap(MultiMap<String, String> paramsMap, boolean optimized) {
    this.optimized = optimized;
    this.paramsMap = optimized
        ? new OptimizedCaseInsensitiveMapWrapper<>(new LinkedHashMap<>())
        : new CaseInsensitiveMapWrapper<>(new LinkedHashMap<>());
    putAll(paramsMap);
    LOGGER.error("Constructor with Multimap paramsMap & boolean optimized, line 80");
  }

  public CaseInsensitiveMultiMap(CaseInsensitiveMultiMap multiMap) {
    this(multiMap, !PRESERVE_HEADER_CASE);
  }

  @Override
  public CaseInsensitiveMultiMap toImmutableMultiMap() {
    LOGGER.error("toImmutableMultiMap, line 85");
    if (this.isEmpty() && emptyCaseInsensitiveMultiMap() != null) {
      return emptyCaseInsensitiveMultiMap();
    }

    return new ImmutableCaseInsensitiveMultiMap(this);
  }

  private static class ImmutableCaseInsensitiveMultiMap extends CaseInsensitiveMultiMap {

    private static final long serialVersionUID = -1048913048598100657L;

    public ImmutableCaseInsensitiveMultiMap(CaseInsensitiveMultiMap caseInsensitiveMultiMap) {
      super(caseInsensitiveMultiMap, caseInsensitiveMultiMap.optimized);
      this.paramsMap = unmodifiableMap(paramsMap);
      LOGGER.error("ImmutableCaseInsensitiveMultiMap with CaseInsensitiveMultiMap parameter, line 100");
    }

    @Override
    public CaseInsensitiveMultiMap toImmutableMultiMap() {
      LOGGER.error("toImmutableMultiMap return this, line 105");
      return this;
    }
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }
}
