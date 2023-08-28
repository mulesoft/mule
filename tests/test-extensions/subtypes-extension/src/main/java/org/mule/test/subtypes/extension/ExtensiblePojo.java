/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Objects;

@Extensible
public class ExtensiblePojo {

  @Parameter
  String myString;

  @Parameter
  List<Integer> numbers;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ExtensiblePojo that = (ExtensiblePojo) o;
    return Objects.equals(myString, that.myString) &&
        Objects.equals(numbers, that.numbers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myString, numbers);
  }
}
