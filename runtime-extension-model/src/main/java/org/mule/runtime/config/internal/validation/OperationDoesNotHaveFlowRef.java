/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;

public class OperationDoesNotHaveFlowRef extends MuleSdkOperationDoesNotHaveForbiddenComponents {

  private static final String FLOW_REF_ELEMENT = "flow-ref";
  private static final ComponentIdentifier FLOW_REF_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_REF_ELEMENT).build();

  @Override
  protected ComponentIdentifier forbiddenComponentIdentifier() {
    return FLOW_REF_IDENTIFIER;
  }
}
