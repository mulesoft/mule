/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.extension.api.introspection.parameter.ParameterModel.RESERVED_NAMES;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import java.util.List;
import java.util.Optional;

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

    private SubTypesMappingContainer subTypesMapping;

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {

        Optional<SubTypesMappingContainer> typesMapping = extensionModel.getModelProperty(SubTypesModelProperty.class)
                .map(p -> new SubTypesMappingContainer(p.getSubTypesMapping()));

        subTypesMapping = typesMapping.isPresent() ? typesMapping.get() : new SubTypesMappingContainer(emptyMap());

        extensionModel.getConfigurationModels().stream()
                .forEach(config -> validateParameters(config.getParameterModels(), config.getName(),
                                                      CONFIGURATION, extensionModel.getName()));

        validateOperations(extensionModel, extensionModel.getOperationModels());
        extensionModel.getConfigurationModels().forEach(config -> validateOperations(extensionModel, config.getOperationModels()));

        validateConnectionProviders(extensionModel, extensionModel.getConnectionProviders());
        extensionModel.getConfigurationModels().forEach(config -> validateConnectionProviders(extensionModel, config.getConnectionProviders()));
    }

    private void validateConnectionProviders(ExtensionModel extensionModel, List<ConnectionProviderModel> providers)
    {
        providers.forEach(provider -> validateParameters(provider.getParameterModels(), provider.getName(),
                                                         CONNECTION_PROVIDER, extensionModel.getName()));
    }

    private void validateOperations(ExtensionModel extensionModel, List<OperationModel> operations)
    {
        operations.forEach(operation -> validateParameters(operation.getParameterModels(), operation.getName(),
                                                           OPERATION, extensionModel.getName()));
    }

    private void validateParameters(List<ParameterModel> parameters, String ownerName, String ownerModelType, String extensionName)
    {
        List<String> parameterHyphenizedNames = parameters.stream().map(p -> hyphenize(p.getName())).collect(toList());

        parameters.stream().forEach(parameterModel -> validateParameter(parameterModel, ownerName, ownerModelType, extensionName));
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
