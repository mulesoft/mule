/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.value.cache;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Objects.hash;
import static org.mule.runtime.app.declaration.internal.utils.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A unique identifier for the values resolved by a {@link ValueProvider} associated with a given component.
 * The ValueProviderCacheId provides a unique hashed value for the resolved values of a value provider taking into consideration
 * all the relevant parts involved in the resolution mechanism.
 *
 * A {@link ValueProviderCacheId} is expected to be immutable, except from it's attributes, which can be modified on any instance.
 * They are intended to add additional information regarding how the {@link ValueProviderCacheId} was generated, but will not
 * affect the value computation.
 *
 * @since 4.2.3, 4.3.0
 */
public class ValueProviderCacheId {

  private final String sourceElementName;
  private final String value;
  private final List<ValueProviderCacheId> parts;
  private final Map<String, String> attributes;

  private ValueProviderCacheId(String sourceElementName, String value, List<ValueProviderCacheId> parts,
                               Map<String, String> attributes) {
    this.sourceElementName = sourceElementName;
    this.parts = new ArrayList<>(parts);
    this.attributes = attributes;
    this.value = value + parts.stream().map(ValueProviderCacheId::getValue).reduce((a, b) -> a + b).orElse(EMPTY);
  }

  public List<ValueProviderCacheId> getParts() {
    return this.parts;
  }

  public String getSourceElementName() {
    return this.sourceElementName;
  }

  public String getValue() {
    return this.value;
  }

  public Map<String, String> getAttributes() {
    return this.attributes;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append(sourceElementName)
        .append(" : ")
        .append(value)
        .append(", attributes: {");
    this.attributes.forEach((k, v) -> stringBuilder.append(lineSeparator()).append(k).append(" : ").append(value));
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ValueProviderCacheId) {
      return Objects.equals(value, ((ValueProviderCacheId) obj).value);
    }
    return false;
  }

  public static class ValueProviderCacheIdBuilder {

    private String sourceElementName;
    private String hashValue;
    private List<ValueProviderCacheId> parts;
    private Map<String, String> attributes;

    public static ValueProviderCacheId aValueProviderCacheId(ValueProviderCacheIdBuilder builder) {
      return builder.build();
    }

    private ValueProviderCacheIdBuilder() {
      this.parts = new LinkedList<>();
      this.attributes = new HashMap<>();
    }

    public static ValueProviderCacheIdBuilder fromElementWithName(String sourceElementName) {
      ValueProviderCacheIdBuilder builder = new ValueProviderCacheIdBuilder();
      builder.sourceElementName = sourceElementName;
      return builder;
    }

    public ValueProviderCacheIdBuilder withHashValueFrom(Object hashedObject) {
      this.hashValue = Integer.toString(hash(hashedObject));
      return this;
    }

    public ValueProviderCacheIdBuilder withHashValue(int hashValue) {
      this.hashValue = Integer.toString(hashValue);
      return this;
    }

    public ValueProviderCacheIdBuilder containing(ValueProviderCacheId... parts) {
      return this.containing(asList(parts));
    }

    public ValueProviderCacheIdBuilder containing(List<ValueProviderCacheId> parts) {
      this.parts.addAll(parts);
      return this;
    }

    public ValueProviderCacheIdBuilder withAttribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
    }

    private ValueProviderCacheId build() {
      checkArgument(this.hashValue != null || !this.parts.isEmpty(), "ValueProviderCacheId must have a hashValue or parts");
      if (this.hashValue == null) {
        this.hashValue = EMPTY;
      }
      return new ValueProviderCacheId(sourceElementName, hashValue, parts, attributes);
    }

  }
}
