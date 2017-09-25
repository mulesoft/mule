/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Validates that all {@link OperationModel operations} specify a valid return type.
 * <p>
 * A return type is considered valid when it's not {@code null} and not a {@link CoreEvent}
 *
 * @since 4.0
 */
public class OperationReturnTypeModelValidator implements ExtensionModelValidator {

  private final List<Class<?>> illegalReturnTypes = ImmutableList.of(CoreEvent.class, Message.class);

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operationModel) {
        if (operationModel.getOutput() == null || operationModel.getOutput().getType() == null) {
          throw missingReturnTypeException(extensionModel, operationModel);
        }

        final Class<Object> returnType = getType(operationModel.getOutput().getType());

        illegalReturnTypes.stream().filter(forbiddenType -> forbiddenType.isAssignableFrom(returnType)).findFirst()
            .ifPresent(forbiddenType -> {
              problemsReporter.addError(new Problem(operationModel, format(
                                                                           "Operation '%s' in Extension '%s' specifies '%s' as a return type. Operations are "
                                                                               + "not allowed to return objects of that type",
                                                                           operationModel.getName(), extensionModel.getName(),
                                                                           forbiddenType.getName())));
            });
      }
    }.walk(extensionModel);
  }

  private IllegalModelDefinitionException missingReturnTypeException(ExtensionModel model, OperationModel operationModel) {
    throw new IllegalOperationModelDefinitionException(format("Operation '%s' in Extension '%s' is missing a return type",
                                                              operationModel.getName(), model.getName()));
  }
}
