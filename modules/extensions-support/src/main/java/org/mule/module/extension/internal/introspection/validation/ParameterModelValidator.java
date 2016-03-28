/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static org.mule.extension.api.introspection.ParameterModel.RESERVED_NAMES;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.metadata.api.model.MetadataType;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;

import java.util.List;

/**
 * Validates that all {@link ParameterModel parameters} provided by the {@link ConfigurationModel configurations},
 * {@link ConnectionProviderModel connection providers} and {@link OperationModel operations}
 * from the {@link ExtensionModel extension} complies with:
 * <ul>
 * <li>The name must not be one of the reserved ones</li>
 * <li>The {@link MetadataType metadataType} must be provided</li>
 * <li>If required, cannot provide a default value</li>
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterModelValidator implements ModelValidator
{

    private static final String CONFIGURATION = "configuration";
    private static final String OPERATION = "operation";
    private static final String CONNECTION_PROVIDER = "connection provider";

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        for (ConfigurationModel configurationModel : extensionModel.getConfigurationModels())
        {
            configurationModel.getParameterModels().forEach(parameterModel -> validateParameter(parameterModel, configurationModel.getName(), CONFIGURATION, extensionModel.getName()));
        }

        validateOperations(extensionModel, extensionModel.getOperationModels());
        extensionModel.getConfigurationModels().forEach(config -> validateOperations(extensionModel, config.getOperationModels()));

        validateConnectionProviders(extensionModel, extensionModel.getConnectionProviders());
        extensionModel.getConfigurationModels().forEach(config -> validateConnectionProviders(extensionModel, config.getConnectionProviders()));
    }

    private void validateConnectionProviders(ExtensionModel extensionModel, List<ConnectionProviderModel> providers)
    {
        providers.forEach(provider -> provider.getParameterModels()
                .forEach(parameterModel -> validateParameter(parameterModel,
                                                             provider.getName(),
                                                             CONNECTION_PROVIDER,
                                                             extensionModel.getName())));
    }

    private void validateOperations(ExtensionModel extensionModel, List<OperationModel> operations)
    {
        operations.forEach(operation -> operation.getParameterModels()
                .forEach(parameterModel -> validateParameter(parameterModel,
                                                             operation.getName(),
                                                             OPERATION,
                                                             extensionModel.getName())));
    }

    private void validateParameter(ParameterModel parameterModel, String ownerName, String ownerModelType, String extensionName)
    {
        if (RESERVED_NAMES.contains(parameterModel.getName()))
        {
            throw new IllegalParameterModelDefinitionException(String.format("The parameter in the %s [%s] from the extension [%s] cannot have the name ['%s'] since it is a reserved one", ownerModelType, ownerName, extensionName, parameterModel.getName()));
        }

        if (parameterModel.getType() == null)
        {
            throw new IllegalParameterModelDefinitionException(String.format("The parameter [%s] in the %s [%s] from the extension [%s] must provide a type", parameterModel.getName(), ownerModelType, ownerName, extensionName));
        }

        if (parameterModel.isRequired() && parameterModel.getDefaultValue() != null)
        {
            throw new IllegalParameterModelDefinitionException(String.format("The parameter [%s] in the %s [%s] from the extension [%s] is required, and must not provide a default value", parameterModel.getName(), ownerModelType, ownerName, extensionName));
        }
    }
}
