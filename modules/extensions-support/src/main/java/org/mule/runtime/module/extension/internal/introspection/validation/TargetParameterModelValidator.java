/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.TARGET_ATTRIBUTE;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;

import com.google.common.base.Joiner;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Validates that no {@link ParameterModel parameters} named {@code target}, since that word is reserved.
 *
 * @since 4.0
 */
public final class TargetParameterModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        List<String> offenses = new LinkedList<>();
        collectOffenses(offenses, extensionModel.getOperationModels());
        extensionModel.getConfigurationModels().forEach(config -> collectOffenses(offenses, config.getOperationModels()));

        if (!offenses.isEmpty())
        {
            throw new IllegalOperationModelDefinitionException(String.format(
                    "Extension '%s' defines operations which have parameters with name '%s', which is a reserved " +
                    "word in the context of an operation parameter. Offending operations are [%s]",
                    extensionModel.getName(), TARGET_ATTRIBUTE, Joiner.on(", ").join(offenses)));
        }
    }

    private void collectOffenses(List<String> offenses, List<OperationModel> operations)
    {
        operations.forEach(operation -> {
            Optional<ParameterModel> offense = operation.getParameterModels().stream()
                    .filter(parameter -> parameter.getName().equals(TARGET_ATTRIBUTE))
                    .findFirst();

            if (offense.isPresent())
            {
                offenses.add(operation.getName());
            }
        });
    }
}
