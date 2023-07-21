/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

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
