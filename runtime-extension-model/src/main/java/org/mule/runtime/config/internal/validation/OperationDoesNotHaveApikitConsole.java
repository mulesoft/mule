/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;

import org.mule.runtime.api.component.ComponentIdentifier;

public class OperationDoesNotHaveApikitConsole extends MuleSdkOperationDoesNotHaveForbiddenComponents {

  private static final ComponentIdentifier APIKIT_CONSOLE_IDENTIFIER =
      builder().namespace("apikit").name("console").build();

  @Override
  protected ComponentIdentifier forbiddenComponentIdentifier() {
    return APIKIT_CONSOLE_IDENTIFIER;
  }
}
