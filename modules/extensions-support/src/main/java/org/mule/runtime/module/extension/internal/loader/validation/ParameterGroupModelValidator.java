/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;

/**
 * Validates that the classes through which parameter groups are implemented are valid
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterGroupModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        validateParameterGroup(groupModel, problemsReporter);
      }
    }.walk(extensionModel);
  }

  private void validateParameterGroup(ParameterGroupModel groupModel, ProblemsReporter problemsReporter) {
    groupModel.getModelProperty(ParameterGroupModelProperty.class).map(ParameterGroupModelProperty::getDescriptor)
        .ifPresent(group -> {
          if (!isInstantiable(group.getType().getDeclaringClass())) {
            problemsReporter
                .addError(new Problem(groupModel,
                                      format("The parameter group of type '%s' should be non abstract with a default constructor.",
                                             group.getType().getDeclaringClass())));
          }
        });
  }
}
