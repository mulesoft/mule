/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OBJECT_STORE;

import org.mule.runtime.ast.graph.api.ComponentAstDependency;

import java.util.function.Predicate;

public class ConfigReferenceParametersStereotypesValidations extends AbstractReferenceParametersStereotypesValidations {

  @Override
  public String getName() {
    return "Config Reference parameters stereotypes";
  }

  @Override
  public String getDescription() {
    return "Config Reference parameters point to declarations of the appropriate stereotype.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  protected Predicate<? super ComponentAstDependency> filter() {
    return missing -> missing.getAllowedStereotypes().stream()
        .anyMatch(st -> st.isAssignableTo(CONFIG)
            || st.isAssignableTo(OBJECT_STORE)
            || st.isAssignableTo(APP_CONFIG));
  }

}
