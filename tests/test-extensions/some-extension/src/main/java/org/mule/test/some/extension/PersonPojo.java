/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Text;

@ExclusiveOptionals(isOneRequired = true)
public class PersonPojo {

  @Parameter
  @Optional
  private String person;

  @Parameter
  @Optional
  @Text
  private String personText;

  public String getName() {
    if (person != null) {
      return person;
    }
    if (personText != null) {
      return personText;
    }
    return "";
  }

  public String getPerson() {
    return person;
  }

  public String getPersonText() {
    return personText;
  }
}
