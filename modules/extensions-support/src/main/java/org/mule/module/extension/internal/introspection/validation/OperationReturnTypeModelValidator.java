/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;

/**
 * Validates that all {@link OperationModel operations} specify
 * a return type
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
                throw new IllegalModelDefinitionException(String.format("Operation '%s' in Extension '%s' is missing a return type",
                                                                        operationModel.getName(), model.getName()));
            }
        }
    }
}
