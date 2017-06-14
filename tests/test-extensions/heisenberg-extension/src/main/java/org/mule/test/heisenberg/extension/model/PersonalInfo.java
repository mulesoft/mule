/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.AGE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PersonalInfo {

  @Parameter
  @Alias("myName")
  @Optional(defaultValue = HEISENBERG)
  @Placement(order = 1)
  private String name;

  @Parameter
  @Optional(defaultValue = AGE)
  @Placement(order = 2)
  private Integer age;

  @Parameter
  @Optional
  @Placement(order = 3)
  private LocalDateTime dateOfConception;

  @Parameter
  @Optional
  @Placement(order = 4)
  private Date dateOfBirth;

  @Parameter
  @Optional
  @DisplayName("Date of decease")
  @Placement(order = 5)
  private Calendar dateOfDeath;

  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(order = 6)
  private Calendar dateOfGraduation;

  @Parameter
  @Placement(order = 7)
  private List<String> knownAddresses;

  public List<String> getKnownAddresses() {
    return knownAddresses;
  }

  public PersonalInfo() {}

  public PersonalInfo(String name, Integer age) {
    this.name = name;
    this.age = age;
  }

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

  public LocalDateTime getDateOfConception() {
    return dateOfConception;
  }

  public void setDateOfConception(LocalDateTime dateOfConception) {
    this.dateOfConception = dateOfConception;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Calendar getDateOfDeath() {
    return dateOfDeath;
  }

  public void setDateOfDeath(Calendar dateOfDeath) {
    this.dateOfDeath = dateOfDeath;
  }

  public Calendar getDateOfGraduation() {
    return dateOfGraduation;
  }

  public void setDateOfGraduation(Calendar dateOfGraduation) {
    this.dateOfGraduation = dateOfGraduation;
  }

  @Override
  public boolean equals(Object obj) {
    return reflectionEquals(obj, this);
  }
}
