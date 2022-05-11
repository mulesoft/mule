/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.privileged.extension.SingletonModelProperty;

import java.util.function.Predicate;

/**
 * Elements declared as singletons via the 'SingletonModelProperty' with 'appliesToFile' as true are not repeated in a config
 * file.
 */
public class SingletonsPerFileAreNotRepeated extends SingletonsAreNotRepeated {

  @Override
  public String getName() {
    return "Singletons per file are not repeated";
  }

  @Override
  public String getDescription() {
    return "Elements declared as singletons via the 'SingletonModelProperty' with 'appliesToFile' as true are not repeated in a config file.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  protected boolean isApplicable(SingletonModelProperty smp) {
    return !super.isApplicable(smp);
  }

  @Override
  protected Predicate<? super ComponentAst> additionalFilter(ComponentAst component) {
    return comp -> comp.getMetadata().getFileName().equals(component.getMetadata().getFileName());
  }

}
