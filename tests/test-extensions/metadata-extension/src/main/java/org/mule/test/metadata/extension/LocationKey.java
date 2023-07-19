/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

public class LocationKey {

  @Optional
  @MetadataKeyPart(order = 1, providedByKeyResolver = false)
  @Parameter
  private String continent;

  @Optional
  @MetadataKeyPart(order = 2)
  @Parameter
  private String country;

  @DisplayName("State | City")
  @MetadataKeyPart(order = 3)
  @Parameter
  private String city;


  public String getContinent() {
    return continent;
  }

  public String getCountry() {
    return country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setContinent(String continent) {
    this.continent = continent;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Override
  public String toString() {
    return String.format("%s|%s|%s", continent, country, city);
  }

  @Override
  public boolean equals(Object obj) {
    return reflectionEquals(this, obj);
  }
}
