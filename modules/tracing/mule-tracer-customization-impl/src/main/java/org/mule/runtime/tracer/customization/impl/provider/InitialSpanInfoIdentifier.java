/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getLocationAsString;

import static java.util.UUID.randomUUID;

import org.mule.runtime.api.component.Component;

import java.util.Objects;

/**
 * An identifier for an {@link org.mule.runtime.tracer.api.span.info.InitialSpanInfo}
 *
 * @since 4.5.0
 */
public class InitialSpanInfoIdentifier {


  private final String stringId;

  public InitialSpanInfoIdentifier(Component component, String suffix, String overriddenName) {
    // If we don't have the location we consider that this is different from
    // any other initial span info, as it can be retrieved from different places.
    if (component.getLocation() == null) {
      this.stringId = randomUUID().toString();
    } else {
      this.stringId =
          getLocationAsString(component.getLocation()) + "-" + defaultString(suffix) + "-" + defaultString(overriddenName);
    }
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
