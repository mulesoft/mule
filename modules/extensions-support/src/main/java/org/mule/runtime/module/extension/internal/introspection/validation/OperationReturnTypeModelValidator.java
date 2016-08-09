/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Validates that all {@link OperationModel operations} specify a valid return type.
 * <p>
 * A return type is considered valid when it's not {@code null} and not a {@link MuleEvent}
 *
 * @since 4.0
 */
public class OperationReturnTypeModelValidator implements ModelValidator {

  private final List<Class<?>> illegalReturnTypes = ImmutableList.of(MuleEvent.class, MuleMessage.class);

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operationModel) {
        if (operationModel.getOutput() == null || operationModel.getOutput().getType() == null) {
          throw missingReturnTypeException(extensionModel, operationModel);
        }

        final Class<Object> returnType = getType(operationModel.getOutput().getType());

        illegalReturnTypes.stream().filter(forbiddenType -> forbiddenType.isAssignableFrom(returnType)).findFirst()
            .ifPresent(forbiddenType -> {
              throw new IllegalOperationModelDefinitionException(String.format(
                                                                               "Operation '%s' in Extension '%s' specifies '%s' as a return type. Operations are "
                                                                                   + "not allowed to return objects of that type",
                                                                               operationModel.getName(), extensionModel.getName(),
                                                                               forbiddenType.getName()));
            });
      }
    }.walk(extensionModel);
  }

  private IllegalModelDefinitionException missingReturnTypeException(ExtensionModel model, OperationModel operationModel) {
    throw new IllegalOperationModelDefinitionException(String.format("Operation '%s' in Extension '%s' is missing a return type",
                                                                     operationModel.getName(), model.getName()));
  }
}
