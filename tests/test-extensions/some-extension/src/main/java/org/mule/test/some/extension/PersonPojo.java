/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
