/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.metadata.java.utils.JavaTypeUtils.getType;

import org.mule.runtime.extension.api.BaseExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * {@link ModelValidator} for metadata keys id of {@link ComponentModel} {@link ParameterModel}
 *
 * @since 4.0
 */
public final class MetadataKeyIdParamModelValidator implements ModelValidator
{

    @Override
    public void validate(ExtensionModel model) throws IllegalModelDefinitionException
    {
        final BaseExtensionWalker baseExtensionWalker = new BaseExtensionWalker()
        {
            @Override
            public void onOperation(HasOperationModels owner, OperationModel model)
            {
                validateMetadataKeyIdParams(model);
            }

            @Override
            public void onSource(HasSourceModels owner, SourceModel model)
            {
                validateMetadataKeyIdParams(model);
            }
        };
        baseExtensionWalker.walk(model);

    }

    private void validateMetadataKeyIdParams(ComponentModel model)
    {
        final Optional<MetadataKeyIdModelProperty> optionalModelProperty = model.getModelProperty(MetadataKeyIdModelProperty.class);

        if (optionalModelProperty.isPresent())
        {
            final MetadataKeyIdModelProperty keyIdModelProperty = optionalModelProperty.get();
            final Class<?> type = getType(keyIdModelProperty.getType());

            final List<ParameterModel> metadataKeyParts = IntrospectionUtils.getMetadataKeyParts(model);
            validateSingleLevelKey(type, metadataKeyParts, model);
        }

    }

    private void validateSingleLevelKey(Class<?> type, List<ParameterModel> metadataKeyParts, ComponentModel model)
    {
        if (metadataKeyParts.size() == 1)
        {
            if (!type.isAssignableFrom(String.class))
            {

                throw new IllegalModelDefinitionException(String.format("The parameter '%s' of the component '%s' of type '%s' is an invalid metadata key id. Metadata keys of one part part" +
                                                                        "should be of String type.", metadataKeyParts.get(0).getName(), model.getName(), type.getSimpleName()));
            }
        }
    }
}
