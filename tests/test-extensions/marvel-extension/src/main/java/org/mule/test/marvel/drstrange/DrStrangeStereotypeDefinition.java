/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

public class DrStrangeStereotypeDefinition implements StereotypeDefinition {

  public static final String DR_STRANGE_STEREOTYPE_NAME = "DR_STRANGE_STEREOTYPE";

  @Override
  public String getName() {
    return DR_STRANGE_STEREOTYPE_NAME;
  }
}
