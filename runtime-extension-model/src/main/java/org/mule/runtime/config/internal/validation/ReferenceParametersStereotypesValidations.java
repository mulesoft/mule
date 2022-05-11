/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.config.internal.validation.FlowRefPointsToExistingFlow.FLOW_REF_IDENTIFIER;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ReferenceParametersStereotypesValidations extends AbstractReferenceParametersStereotypesValidations {

  @Override
  public String getName() {
    return "Reference parameters stereotypes";
  }

  @Override
  public String getDescription() {
    return "Reference parameters point to declarations of the appropriate stereotype.";
  }

  @Override
  public Level getLevel() {
    // Keep backwards compatibility with some scenarios that are expected to deploy and fail at runtime rather than deployment
    return WARN;
  }

  @Override
  protected Predicate<? super ComponentAstDependency> filter(ArtifactAst artifact) {
    return missing ->
    // flow-ref is already validated by FlowRefPointsToExistingFlow
    !missing.getComponent().getIdentifier().equals(FLOW_REF_IDENTIFIER)
        && missing.getAllowedStereotypes().stream()
            .noneMatch(st -> st.isAssignableTo(CONFIG)
                || st.isAssignableTo(APP_CONFIG));
  }

}
