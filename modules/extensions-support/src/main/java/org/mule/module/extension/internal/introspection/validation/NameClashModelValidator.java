/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.Described;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.metadata.api.model.ObjectType;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Validates names clashes in the model by comparing:
 * <ul>
 * <li>The {@link Described#getName()} value of all the {@link ConfigurationModel}, {@link OperationModel} and {@link ConnectionProviderModel}</li>
 * <li>Makes sure that there no two {@link ParameterModel}s with the same name but different types, for those which represent an object</li>
 * <li>Makes sure that no {@link ConfigurationModel}, {@link OperationModel} or {@link ConnectionProviderModel} have parameters with repeated name</li>
 * </ul>
 *
 * @since 4.0
 */
public final class NameClashModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel model) throws IllegalModelDefinitionException
    {
        new ValidationDelegate(model).validate(model);
    }

    private class ValidationDelegate
    {

        public static final String CONFIGURATIONS = "configurations";
        public static final String OPERATIONS = "operations";
        public static final String CONNECTION_PROVIDERS = "connection providers";
        public static final String COMPLEX_TYPE_PARAMETERS = "complex type parameters";
        private final ExtensionModel extensionModel;
        private final Set<String> configurationNames = new HashSet<>();
        private final Set<String> operationNames = new HashSet<>();
        private final Set<String> connectionProviderNames = new HashSet<>();
        private final Multimap<String, TopLevelParameter> topLevelParameters = LinkedListMultimap.create();

        public ValidationDelegate(ExtensionModel extensionModel)
        {
            this.extensionModel = extensionModel;
        }

        private void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
        {
            extensionModel.getConfigurationModels().stream().forEach(configurationModel -> validate(configurationModel.getParameterModels(),
                                                                                                    configurationNames,
                                                                                                    configurationModel.getName(),
                                                                                                    "configuration"));

            extensionModel.getOperationModels().stream().forEach(operationModel -> validate(operationModel.getParameterModels(),
                                                                                            operationNames,
                                                                                            operationModel.getName(),
                                                                                            "operation"));

            // Check clash between each operation and its parameters type
            extensionModel.getOperationModels().stream().forEach(operationModel -> {
                operationModel.getParameterModels().stream().forEach(parameterModel -> {
                    validateClash(operationModel.getName(),
                                  getType(parameterModel.getType()).getName(),
                                  "operation",
                                  "argument");
                });
            });

            // Check clashes between each operation argument and the connection providers
            extensionModel.getConnectionProviders().stream().forEach(connectionProviderModel -> {
                extensionModel.getOperationModels().stream().forEach(operationModel -> {
                    operationModel.getParameterModels().stream().forEach(parameterModel -> {
                        validateClash(connectionProviderModel.getName(),
                                      getType(parameterModel.getType()).getName(),
                                      "connection provider",
                                      String.format("operation's (%s) parameter", operationModel.getName()));
                    });
                });
            });

            // Check clashes between each operation argument and the configs
            extensionModel.getConfigurationModels().stream().forEach(configurationModel -> {
                extensionModel.getOperationModels().stream().forEach(operationModel -> {
                    operationModel.getParameterModels().stream().forEach(parameterModel -> {
                        validateClash(configurationModel.getName(),
                                      getType(parameterModel.getType()).getName(),
                                      "configuration",
                                      String.format("operation's (%s) parameter", operationModel.getName()));
                    });
                });
            });

            extensionModel.getConnectionProviders().stream().forEach(providerModel -> validate(providerModel.getParameterModels(),
                                                                                               connectionProviderNames,
                                                                                               providerModel.getName(),
                                                                                               "connection provider"));

            validateClashes(configurationNames, operationNames, CONFIGURATIONS, OPERATIONS);
            validateClashes(configurationNames, connectionProviderNames, CONFIGURATIONS, CONNECTION_PROVIDERS);
            validateClashes(operationNames, connectionProviderNames, OPERATIONS, CONNECTION_PROVIDERS);

            Set<String> parameterNames = topLevelParameters.keySet();
            validateClashes(configurationNames, parameterNames, CONFIGURATIONS, COMPLEX_TYPE_PARAMETERS);
            validateClashes(operationNames, parameterNames, OPERATIONS, COMPLEX_TYPE_PARAMETERS);
            validateClashes(connectionProviderNames, parameterNames, CONNECTION_PROVIDERS, COMPLEX_TYPE_PARAMETERS);
        }

        private void validate(List<ParameterModel> parameters, Set<String> accumulator, String ownerName, String ownerType)
        {
            Set<String> repeatedParameters = getRepeatedParameters(parameters);
            if (!repeatedParameters.isEmpty())
            {
                throw new IllegalModelDefinitionException(format("Extension '%s' defines the %s '%s' which has parameters " +
                                                                 "with repeated names. Offending parameters are: [%s]",
                                                                 extensionModel.getName(), ownerType, ownerName, Joiner.on(",").join(repeatedParameters)));
            }

            validateTopLevelParameters(parameters, ownerName, ownerType);

            if (!accumulator.add(ownerName))
            {
                throw new IllegalModelDefinitionException(format("Extension '%s' defines more than one %s of name '%s'. Please make sure %s names are unique",
                                                                 extensionModel.getName(), ownerType, ownerName, ownerType));
            }
        }


        private void validateTopLevelParameters(List<ParameterModel> parameters, String ownerName, String ownerType)
        {
            parameters.stream()
                    .filter(parameter -> parameter.getType() instanceof ObjectType)
                    .forEach(parameter -> {
                        final Class<?> parameterType = getType(parameter.getType());
                        Collection<TopLevelParameter> foundParameters = topLevelParameters.get(parameter.getName());
                        if (CollectionUtils.isEmpty(foundParameters))
                        {
                            topLevelParameters.put(parameter.getName(), new TopLevelParameter(parameter, ownerName, ownerType));
                        }
                        else
                        {
                            Optional<TopLevelParameter> repeated = foundParameters.stream()
                                    .filter(topLevelParameter -> !topLevelParameter.type.equals(parameterType))
                                    .findFirst();

                            if (repeated.isPresent())
                            {
                                TopLevelParameter tp = repeated.get();
                                throw new IllegalModelDefinitionException(format("Extension '%s' defines a %s of name '%s' which contains a parameter of complex type '%s'. However, " +
                                                                                 "%s of name '%s' defines a parameter of the same name but type '%s'. Complex parameter of different types cannot have the same name.",
                                                                                 extensionModel.getName(), ownerType, ownerName, parameterType, tp.ownerType, tp.owner, tp.type.getName()));
                            }
                        }
                    });
        }

        private Set<String> getRepeatedParameters(List<ParameterModel> parameters)
        {
            Set<String> names = new HashSet<>();
            Set<String> repeatedNames = parameters.stream()
                    .filter(parameter -> !names.add(parameter.getName()))
                    .map(ParameterModel::getName)
                    .collect(toSet());

            return repeatedNames;
        }

        private void validateClashes(Set<String> set1, Set<String> set2, String type1, String type2)
        {
            Set<String> intersection = Sets.intersection(set1, set2);
            if (!intersection.isEmpty())
            {
                throw new IllegalModelDefinitionException(format("Extension '%s' has %s and %s with the same name. Offending names are: [%s]",
                                                                 extensionModel.getName(), type1, type2, Joiner.on(", ").join(intersection)));
            }
        }

        private void validateClash(String existingNamingModel, String newNamingModel, String typeOfExistingNamingModel, String typeOfNewNamingModel)
        {
            if (StringUtils.equalsIgnoreCase(existingNamingModel, newNamingModel))
            {
                throw new IllegalModelDefinitionException(format("Extension '%s' has a %s named '%s' with an %s type named equally.",
                                                                 extensionModel.getName(), typeOfExistingNamingModel, existingNamingModel, typeOfNewNamingModel));
            }
        }
    }


    private class TopLevelParameter
    {

        private String owner;
        private String ownerType;
        private Class<?> type;

        private TopLevelParameter(ParameterModel parameterModel, String owner, String ownerType)
        {
            this.owner = owner;
            this.ownerType = ownerType;
            type = getType(parameterModel.getType());
        }
    }
}
