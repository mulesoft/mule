/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.utils.JavaTypeUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;

import java.util.List;
import java.util.Map;

public class MetadataComponentModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException
    {
        doValidate(extensionModel, extensionModel.getOperationModels());
        extensionModel.getConfigurationModels().forEach(config -> doValidate(extensionModel, config.getOperationModels()));
        doValidate(extensionModel, extensionModel.getSourceModels());
    }

    private void doValidate(ExtensionModel extensionModel, List<? extends ComponentModel> operations)
    {
        for (ComponentModel operationModel : operations)
        {
            validateMetadataReturnType(extensionModel, operationModel);
        }
    }

    private void validateMetadataReturnType(ExtensionModel extensionModel, ComponentModel componentModel)
    {
        RuntimeComponentModel component = (RuntimeComponentModel) componentModel;
        MetadataType returnMetadataType = component.getReturnType();
        Class returnType = JavaTypeUtils.getType(returnMetadataType);

        if (returnMetadataType instanceof ObjectType || returnMetadataType instanceof DictionaryType)
        {
            validateReturnType(extensionModel, component, returnType);
        }
    }

    private void validateReturnType(ExtensionModel extensionModel, RuntimeComponentModel component, Class<?> returnType)
    {
        if (Object.class.equals(returnType) || Map.class.isAssignableFrom(returnType))
        {
            if (component.getMetadataResolverFactory().getOutputResolver() instanceof NullMetadataResolver)
            {

                throw new IllegalOperationModelDefinitionException(String.format("Component '%s' in Extension '%s' specifies '%s' as a return type. Operations with " +
                                                                                 "return type such as Object or Map must have defined a not null MetadataOutputResolver",
                                                                                 component.getName(), extensionModel.getName(), returnType.getName()));
            }
        }
    }
}
