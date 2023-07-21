/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.extension;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Allows to modify the rules used for building the {@link ComponentLocation} for the component with this property.
 *
 * @since 4.4
 */
public class CustomLocationPartModelProperty implements ModelProperty {

  private static final long serialVersionUID = 4221736527017299553L;

  private final String locationPart;
  private final boolean indexed;

  public CustomLocationPartModelProperty(String locationPart) {
    this(locationPart, true);
  }

  public CustomLocationPartModelProperty(String locationPart, boolean indexed) {
    this.locationPart = locationPart;
    this.indexed = indexed;
  }

  /**
   * @return the location part that will be used, overriding the default that would otherwise be set by the runtime when building
   *         the {@link ComponentLocation}.
   */
  public String getLocationPart() {
    return locationPart;
  }

  /**
   * @return whether an index is followed by the value returned by {@link #getLocationPart()} in the built
   *         {@link ComponentLocation}.
   */
  public boolean isIndexed() {
    return indexed;
  }

  @Override
  public String getName() {
    return "customLocationPart";
  }

  @Override
  public boolean isPublic() {
    return true;
  }

}
