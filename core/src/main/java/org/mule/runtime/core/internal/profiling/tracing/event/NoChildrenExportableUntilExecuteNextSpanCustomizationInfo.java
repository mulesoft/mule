/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event;

import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.EXECUTE_NEXT;

import static java.util.Collections.singleton;

import org.mule.runtime.api.component.Component;

import java.util.Set;

/**
 * A {@link NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo} that will not export any spans until an execute-next.
 *
 * @since 4.5.0
 */
public class NoChildrenExportableUntilExecuteNextSpanCustomizationInfo extends
    NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo {

  private boolean policySpan = false;

  /**
   * @param component  the component corresponding to the span to be created.
   * @param policySpan if this is a policySpan
   */
  public NoChildrenExportableUntilExecuteNextSpanCustomizationInfo(
                                                                   Component component, boolean policySpan) {
    super(component);
    this.policySpan = policySpan;

  }

  /**
   * @param component the component corresponding to the span to be created.
   */
  public NoChildrenExportableUntilExecuteNextSpanCustomizationInfo(
                                                                   Component component) {
    super(component);
  }

  @Override
  public Set<String> noExportUntil() {
    return singleton(EXECUTE_NEXT);
  }

  @Override
  public boolean isPolicySpan() {
    return policySpan;
  }
}
