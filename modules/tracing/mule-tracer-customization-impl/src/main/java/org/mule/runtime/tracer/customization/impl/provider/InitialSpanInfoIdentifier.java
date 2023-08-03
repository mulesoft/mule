/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.Objects;

/**
 * An identifier for an {@link org.mule.runtime.tracer.api.span.info.InitialSpanInfo}
 *
 * @since 4.5.0
 */
public class InitialSpanInfoIdentifier {

  private final String stringId;

  public InitialSpanInfoIdentifier(String location, String suffix, String overriddenName) {
    this.stringId =
        location + "-" + defaultString(suffix) + "-" + defaultString(overriddenName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    InitialSpanInfoIdentifier that = (InitialSpanInfoIdentifier) o;
    return Objects.equals(stringId, that.stringId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringId);
  }
}
