/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.api.cache;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A unique identifier for the Metadata obtained for a given Component. The MetadataCacheId provides a unique hashed value for the
 * configuration of a component taking into consideration all the relevant parts involved in the Metadata resolution mechanism.
 * <p>
 * Two elements having the same {@link MetadataCacheId} will be equal regarding all the types inferred from its configuration.
 *
 * @since 4.1.4, 4.2.0
 */
public class MetadataCacheId {

  private final String value;
  private final String sourceElementName;
  private final List<MetadataCacheId> parts;

  public MetadataCacheId(List<MetadataCacheId> parts, String sourceElementName) {
    this.parts = parts;
    this.sourceElementName = sourceElementName;
    this.value = parts.stream().map(MetadataCacheId::getValue).reduce((a, b) -> a + b)
        .orElseThrow(() -> new IllegalArgumentException(
                                                        format("At least one part is required for a Metadata Cache ID, but none was found for element '%s'",
                                                               sourceElementName)));
  }

  public MetadataCacheId(String value, String sourceElementName) {
    this.value = value;
    this.sourceElementName = sourceElementName;
    this.parts = Collections.emptyList();
  }

  public MetadataCacheId(int value, String sourceElementName) {
    this(String.valueOf(value), sourceElementName);
  }

  public String getValue() {
    return value;
  }

  public List<MetadataCacheId> getParts() {
    return parts;
  }

  public Optional<String> getSourceElementName() {
    return Optional.ofNullable(sourceElementName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataCacheId that = (MetadataCacheId) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return '(' + value + ',' + sourceElementName + ')';
  }

}
