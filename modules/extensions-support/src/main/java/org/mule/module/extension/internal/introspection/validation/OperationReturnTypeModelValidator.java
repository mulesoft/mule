/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import org.mule.api.MuleEvent;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;

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
    public void validate(ExtensionModel model) throws IllegalModelDefinitionException
    {
        for (OperationModel operationModel : model.getOperationModels())
        {
            if (operationModel.getReturnType() == null)
            {
                throw missingReturnTypeException(model, operationModel);
            }

            Class<?> returnType = operationModel.getReturnType().getRawType();
            if (returnType == null)
            {
                throw missingReturnTypeException(model, operationModel);
            }

            if (MuleEvent.class.isAssignableFrom(returnType))
            {
                throw new IllegalOperationModelDefinitionException(String.format("Operation '%s' in Extension '%s' specifies '%s' as a return type. Operations are " +
                                                                 "not allowed to return objects of that type",
                                                                 operationModel.getName(), model.getName(), MuleEvent.class.getName()));
            }
        }
    }

    private IllegalModelDefinitionException missingReturnTypeException(ExtensionModel model, OperationModel operationModel)
    {
        throw new IllegalOperationModelDefinitionException(String.format("Operation '%s' in Extension '%s' is missing a return type",
                                                                         operationModel.getName(), model.getName()));
    }
}
