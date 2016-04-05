/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static org.mule.extension.api.introspection.ParameterModel.RESERVED_NAMES;
import static org.mule.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.model.SubTypesMapper;
import org.mule.module.extension.internal.model.property.SubTypesModelProperty;
import org.mule.module.extension.internal.util.IntrospectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        Optional<SubTypesModelProperty> subTypesDeclaration = extensionModel.getModelProperty(SubTypesModelProperty.class);
        SubTypesMapper typeMapping = subTypesDeclaration.isPresent() ? subTypesDeclaration.get().getSubTypesMapping() : new SubTypesMapper(Collections.emptyMap());

        extensionModel.getConfigurationModels().stream()
                .forEach(config -> validateParameters(config.getParameterModels(), config.getName(),
                                                      CONFIGURATION, extensionModel.getName(), typeMapping));

        validateOperations(extensionModel, extensionModel.getOperationModels(), typeMapping);
        extensionModel.getConfigurationModels().forEach(config -> validateOperations(extensionModel, config.getOperationModels(), typeMapping));

        validateConnectionProviders(extensionModel, extensionModel.getConnectionProviders(), typeMapping);
        extensionModel.getConfigurationModels().forEach(config -> validateConnectionProviders(extensionModel, config.getConnectionProviders(), typeMapping));
    }

    private void validateConnectionProviders(ExtensionModel extensionModel, List<ConnectionProviderModel> providers, SubTypesMapper typeMapping)
    {
        providers.forEach(provider -> validateParameters(provider.getParameterModels(), provider.getName(),
                                                         CONNECTION_PROVIDER, extensionModel.getName(), typeMapping));
    }

    private void validateOperations(ExtensionModel extensionModel, List<OperationModel> operations, SubTypesMapper typeMapping)
    {
        operations.forEach(operation -> validateParameters(operation.getParameterModels(), operation.getName(),
                                                           OPERATION, extensionModel.getName(), typeMapping));
    }

    private void validateParameters(List<ParameterModel> parameters, String ownerName, String ownerModelType, String extensionName, SubTypesMapper typeMapping)
    {
        List<String> parameterHyphenizedNames = parameters.stream().map(p -> hyphenize(p.getName())).collect(Collectors.toList());

        parameters.stream().forEach(parameterModel -> {

            validateParameter(parameterModel, ownerName, ownerModelType, extensionName);

            validateNameCollisionWithSubtypes(ownerName, ownerModelType, extensionName, typeMapping, parameterHyphenizedNames, parameterModel);
        });

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

    private void validateNameCollisionWithSubtypes(String ownerName, String ownerModelType, String extensionName, SubTypesMapper typeMapping, List<String> parameterNames, ParameterModel parameterModel)
    {
        Optional<MetadataType> subTypeWithNameCollision = typeMapping.getSubTypes(parameterModel.getType()).stream()
                .filter(subtype -> parameterNames.contains(hyphenize(IntrospectionUtils.getAliasName(subtype)))).findFirst();

        if (subTypeWithNameCollision.isPresent())
        {
            throw new IllegalParameterModelDefinitionException(
                    String.format("The parameter [%s] in the %s [%s] from the extension [%s] can't have the same name as the ClassName or Alias of the declared subType [%s] for parameter [%s]",
                                  IntrospectionUtils.getAliasName(subTypeWithNameCollision.get()), ownerModelType, ownerName, extensionName,
                                  JavaTypeUtils.getType(subTypeWithNameCollision.get()).getSimpleName(), parameterModel.getName()));
        }
    }
}
