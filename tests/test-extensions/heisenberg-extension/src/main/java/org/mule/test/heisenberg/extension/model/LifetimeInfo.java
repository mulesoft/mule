/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import static org.mule.test.heisenberg.extension.HeisenbergExtension.PERSONAL_INFORMATION_GROUP_NAME;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

public class LifetimeInfo {

  @Optional
  @Placement(group = PERSONAL_INFORMATION_GROUP_NAME, order = 3)
  private LocalDateTime dateOfConception;

  @Placement(group = PERSONAL_INFORMATION_GROUP_NAME, order = 4)
  private Date dateOfBirth;

  @DisplayName("Date of decease")
  @Placement(group = PERSONAL_INFORMATION_GROUP_NAME, order = 5)
  private Calendar dateOfDeath;

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

  public LocalDateTime getDateOfConception() {
    return dateOfConception;
  }

  public void setDateOfConception(LocalDateTime dateOfConception) {
    this.dateOfConception = dateOfConception;
  }
}
