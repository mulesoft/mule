/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.tck.testmodels.fruit.Fruit;

public class VeganFidelityOperation {

  public String tryEat(@XmlHints(allowReferences = false) Object food) {
    if (food instanceof Fruit) {
      return "tasty " + food.getClass().getSimpleName();
    }

    throw new IllegalArgumentException("I SHALL NEVER EAT " + food.toString());
  }

}
