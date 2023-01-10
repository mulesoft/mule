/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;

import org.mule.runtime.api.component.ComponentIdentifier;

public class OperationDoesNotHaveApikitRouter extends MuleSdkOperationDoesNotHaveForbiddenComponents {

  private static final ComponentIdentifier APIKIT_ROUTER_IDENTIFIER =
      builder().namespace("apikit").name("router").build();

  @Override
  protected ComponentIdentifier forbiddenComponentIdentifier() {
    return APIKIT_ROUTER_IDENTIFIER;
  }
}
