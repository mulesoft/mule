/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.PERSONAL_INFORMATION_GROUP_NAME;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class PersonalInfo {

  @Parameter
  @Alias("myName")
  @Optional(defaultValue = HEISENBERG)
  @Placement(group = PERSONAL_INFORMATION_GROUP_NAME, order = 1)
  private String name;

  @Parameter
  @Optional(defaultValue = AGE)
  @Placement(group = PERSONAL_INFORMATION_GROUP_NAME, order = 2)
  private Integer age;

  public PersonalInfo(String name, Integer age) {
    this.name = name;
    this.age = age;
  }

  public PersonalInfo() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
