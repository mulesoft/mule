/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

@TypeDsl(substitutionGroup = "mule:abstract-shared-extension")
public class MuleSGPojo {

  @Parameter
  boolean innerParameter;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MuleSGPojo that = (MuleSGPojo) o;
    return innerParameter == that.innerParameter;
  }

  @Override
  public int hashCode() {
    return Objects.hash(innerParameter);
  }
}
