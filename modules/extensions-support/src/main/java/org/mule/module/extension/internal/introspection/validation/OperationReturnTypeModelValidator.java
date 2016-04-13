/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import org.mule.api.MuleEvent;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.operation.OperationModel;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;

import java.util.List;

/**
 * Validates that all {@link OperationModel operations} specify
 * a valid return type.
 * <p>
 * A return type is considered valid when it's not {@code null} and
 * not a {@link MuleEvent}
 *
 * @since 4.0
 */
public class OperationReturnTypeModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        doValidate(extensionModel, extensionModel.getOperationModels());
        extensionModel.getConfigurationModels().forEach(config -> doValidate(extensionModel, config.getOperationModels()));
    }

    private void doValidate(ExtensionModel extensionModel, List<OperationModel> operations)
    {
        for (OperationModel operationModel : operations)
        {
            if (operationModel.getReturnType() == null)
            {
                throw missingReturnTypeException(extensionModel, operationModel);
            }

            Class<?> returnType = getType(operationModel.getReturnType());
            if (returnType == null)
            {
                throw missingReturnTypeException(extensionModel, operationModel);
            }

            if (MuleEvent.class.isAssignableFrom(returnType))
            {
                throw new IllegalOperationModelDefinitionException(String.format("Operation '%s' in Extension '%s' specifies '%s' as a return type. Operations are " +
                                                                                 "not allowed to return objects of that type",
                                                                                 operationModel.getName(), extensionModel.getName(), MuleEvent.class.getName()));
            }
        }
    }

    private IllegalModelDefinitionException missingReturnTypeException(ExtensionModel model, OperationModel operationModel)
    {
        throw new IllegalOperationModelDefinitionException(String.format("Operation '%s' in Extension '%s' is missing a return type",
                                                                         operationModel.getName(), model.getName()));
    }
}
