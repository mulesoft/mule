/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;

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
            if (operationModel.getOutput() == null || operationModel.getOutput().getType() == null)
            {
                throw missingReturnTypeException(extensionModel, operationModel);
            }

            MetadataType returnType = operationModel.getOutput().getType();
            if (returnType.getMetadataFormat().equals(JAVA) &&
                MuleEvent.class.isAssignableFrom(getType(returnType)))
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
