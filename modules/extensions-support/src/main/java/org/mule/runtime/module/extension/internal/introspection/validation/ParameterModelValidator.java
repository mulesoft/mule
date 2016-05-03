/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.introspection.parameter.ParameterModel.RESERVED_NAMES;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAliasName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.Described;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import com.google.common.collect.ImmutableMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Validates that all {@link ParameterModel parameters} provided by the {@link ConfigurationModel configurations},
 * {@link ConnectionProviderModel connection providers} and {@link OperationModel operations}
 * from the {@link ExtensionModel extension} complies with:
 * <ul>
 * <li>The name must not be one of the reserved ones</li>
 * <li>The {@link MetadataType metadataType} must be provided</li>
 * <li>If required, cannot provide a default value</li>
 * <li>The {@link Class} of the parameter must be valid too, that implies that the class shouldn't contain any field with a reserved name.
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
    private Map<MetadataType, MetadataType> importedTypes;

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        MetadataTypeVisitor visitor = new MetadataTypeVisitor()
        {
            private Set<Class<?>> visitedClasses = new HashSet<>();

            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                dictionaryType.getKeyType().accept(this);
                dictionaryType.getValueType().accept(this);
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                arrayType.getType().accept(this);
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                Class<?> type = getType(objectType);

                if (visitedClasses.add(type) && isInstantiable(type))
                {
                    for (ObjectFieldType objectFieldType : objectType.getFields())
                    {
                        Class<?> fieldType = getType(objectFieldType.getValue());

                        String fieldName = getAliasName(objectFieldType, objectFieldType.getKey().getName().getLocalPart());
                        if (RESERVED_NAMES.contains(fieldName))
                        {
                            throw new IllegalParameterModelDefinitionException(
                                    String.format("The field named '%s' [%s] from class [%s] cannot have that name since it is a reserved one",
                                                  fieldName, fieldType.getName(), type.getName()));
                        }
                        else
                        {
                            objectFieldType.getValue().accept(this);
                        }
                    }
                }
            }
        };

        Optional<SubTypesMappingContainer> typesMapping = extensionModel.getModelProperty(SubTypesModelProperty.class)
                .map(p -> new SubTypesMappingContainer(p.getSubTypesMapping()));
        subTypesMapping = typesMapping.isPresent() ? typesMapping.get() : new SubTypesMappingContainer(emptyMap());

        Optional<Map<MetadataType, MetadataType>> typeImports = extensionModel.getModelProperty(ImportedTypesModelProperty.class)
                .map(ImportedTypesModelProperty::getImportedTypes);
        importedTypes = typeImports.isPresent() ? typeImports.get() : ImmutableMap.of();

        extensionModel.getConfigurationModels().stream()
                .forEach(config -> validateParameters(config.getParameterModels(), visitor, config.getName(),
                                                      CONFIGURATION, extensionModel.getName()));

        validateOperations(extensionModel, extensionModel.getOperationModels(), visitor);
        extensionModel.getConfigurationModels().forEach(config -> validateOperations(extensionModel, config.getOperationModels(), visitor));

        validateConnectionProviders(extensionModel, extensionModel.getConnectionProviders(), visitor);
        extensionModel.getConfigurationModels().forEach(config -> validateConnectionProviders(extensionModel, config.getConnectionProviders(), visitor));
    }

    private void validateConnectionProviders(ExtensionModel extensionModel, List<ConnectionProviderModel> providers, MetadataTypeVisitor visitor)
    {
        providers.forEach(provider -> validateParameters(provider.getParameterModels(), visitor, provider.getName(),
                                                         CONNECTION_PROVIDER, extensionModel.getName()));
    }

    private void validateOperations(ExtensionModel extensionModel, List<OperationModel> operations, MetadataTypeVisitor visitor)
    {
        operations.forEach(operation -> validateParameters(operation.getParameterModels(), visitor, operation.getName(),
                                                           OPERATION, extensionModel.getName()));
    }

    private void validateParameters(List<ParameterModel> parameters, MetadataTypeVisitor visitor, String ownerName, String ownerModelType, String extensionName)
    {
        parameters.stream().forEach(parameterModel -> {
            validateParameter(parameterModel, visitor, ownerName, ownerModelType, extensionName);
            validateNameCollisionWithTypes(parameterModel, ownerName, ownerModelType, extensionName,
                                           parameters.stream().map(Described::getName).collect(toList()));
        });
    }

    private void validateParameter(ParameterModel parameterModel, MetadataTypeVisitor visitor, String ownerName, String ownerModelType, String extensionName)
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

        parameterModel.getType().accept(visitor);
    }

    private void validateNameCollisionWithTypes(ParameterModel parameterModel, String ownerName, String ownerModelType, String extensionName, List<String> parameterNames)
    {
        if (subTypesMapping.getSubTypes(parameterModel.getType()).isEmpty() && !importedTypes.containsKey(parameterModel.getType()))
        {
            return;
        }

        if (parameterNames.contains(getAliasName(parameterModel.getType())))
        {
            throw new IllegalParameterModelDefinitionException(
                    String.format("The parameter [%s] in the %s [%s] from the extension [%s] can't have the same name as the ClassName or Alias of its declared type [%s]",
                                  getAliasName(parameterModel.getType()), ownerModelType, ownerName, extensionName,
                                  getType(parameterModel.getType()).getSimpleName()));
        }

        Optional<MetadataType> subTypeWithNameCollision = subTypesMapping.getSubTypes(parameterModel.getType()).stream()
                .filter(subtype -> parameterNames.contains(getAliasName(subtype))).findFirst();
        if (subTypeWithNameCollision.isPresent())
        {
            throw new IllegalParameterModelDefinitionException(
                    String.format("The parameter [%s] in the %s [%s] from the extension [%s] can't have the same name as the ClassName or Alias of the declared subType [%s] for parameter [%s]",
                                  getAliasName(subTypeWithNameCollision.get()), ownerModelType, ownerName, extensionName,
                                  getType(subTypeWithNameCollision.get()).getSimpleName(), parameterModel.getName()));
        }

    }

}
