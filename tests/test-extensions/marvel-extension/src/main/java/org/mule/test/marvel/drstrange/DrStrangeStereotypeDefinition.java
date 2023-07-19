/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
