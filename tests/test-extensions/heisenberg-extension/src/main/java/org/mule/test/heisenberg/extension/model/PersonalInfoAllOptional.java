/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.List;

public class PersonalInfoAllOptional {

  @Parameter
  @Optional(defaultValue = AGE)
  @Placement(order = 1)
  private Integer age;

  @Parameter
  @Optional
  @Placement(order = 2)
  private List<String> knownAddresses;

  public List<String> getKnownAddresses() {
    return knownAddresses;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  @Override
  public boolean equals(Object obj) {
    return reflectionEquals(obj, this);
  }
}
