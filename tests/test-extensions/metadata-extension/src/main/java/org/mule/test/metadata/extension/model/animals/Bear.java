/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.model.animals;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;

import static org.mule.test.metadata.extension.model.animals.AnimalClade.MAMMAL;

@XmlHints(allowTopLevelDefinition = true)
public class Bear implements Animal {

  private String bearName;

  public String getBearName() {
    return bearName;
  }

  public void setBearName(String bearName) {
    this.bearName = bearName;
  }

  @Override
  public AnimalClade clade() {
    return MAMMAL;
  }
}
