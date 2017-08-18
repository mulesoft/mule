/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;

/**
 * Validates that all the paged {@link OperationModel operations} don't receive a {@link Connection} parameter.
 *
 * @since 4.0
 */
public class PagedOperationModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operationModel) {
        boolean hasConnectionParameter = operationModel.getModelProperty(ImplementingMethodModelProperty.class)
            .map(implementingMethodModelProperty -> new MethodWrapper(implementingMethodModelProperty.getMethod())
                .getParametersAnnotatedWith(Connection.class).size() > 0)
            .orElse(false);
        if (hasConnectionParameter && operationModel.getModelProperty(PagedOperationModelProperty.class).isPresent()) {
          problemsReporter.addError(new Problem(operationModel, format(
                                                                       "Operation '%s' in Extension '%s' is paged and has a parameter annotated with '%s' at the same time. Paged operation shouldn't have a connection parameter.",
                                                                       operationModel.getName(), extensionModel.getName(),
                                                                       Connection.class.getName())));
        }
      }
    }.walk(extensionModel);
  }
}
