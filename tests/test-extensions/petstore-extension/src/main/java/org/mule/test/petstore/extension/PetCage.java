/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PetCage {

  @Parameter
  @Optional
  Map<String, Integer> birds;

  @Parameter
  @Optional
  List<String> ammenities;

  @Parameter
  @Optional
  TlsContextFactory tls;

  public Map<String, Integer> getBirds() {
    return birds;
  }

  public List<String> getAmmenities() {
    return ammenities;
  }

  public TlsContextFactory getTls() {
    return tls;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PetCage petCage = (PetCage) o;
    return Objects.equals(birds, petCage.birds) &&
        Objects.equals(ammenities, petCage.ammenities) &&
        Objects.equals(tls, petCage.tls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(birds, ammenities, tls);
  }
}
