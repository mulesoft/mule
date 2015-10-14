/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import static org.mule.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.extension.annotation.api.ContentMetadataParameters;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.module.extension.internal.util.MuleExtensionUtils;

import java.lang.reflect.Method;

/**
 * Enriches operations which were defined in methods annotated with {@link ContentMetadataParameters} so that
 * parameters related to {@link ContentType} are added. In particular, it adds two parameters named
 * {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} and {@link ExtensionProperties#ENCODING_PARAMETER_NAME}.
 * <p>
 * Both attributes are optional, have no default value and accept expressions.
 *
 * @since 4.0
 */
public final class ContentMetadataModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        describingContext.getDeclarationDescriptor().getDeclaration().getOperations().forEach(operation -> {
            Method method = MuleExtensionUtils.getImplementingMethod(operation);
            if (method != null)
            {
                ContentMetadataParameters annotation = method.getAnnotation(ContentMetadataParameters.class);
                if (annotation != null)
                {
                    if (IntrospectionUtils.isVoid(method))
                    {
                        throw new IllegalModelDefinitionException(String.format(
                                "Operation '%s' of extension '%s' is void yet requires the ability to change the content metadata." +
                                " Mutating the content metadata requires an operation with a return type.",
                                operation.getName(),describingContext.getDeclarationDescriptor().getDeclaration().getName()));
                    }

                    operation.addParameter(newParameter(MIME_TYPE_PARAMETER_NAME, "The mime type of the payload that this operation outputs."));
                    operation.addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding of the payload that this operation outputs."));
                }

            }
        });
    }

    private ParameterDeclaration newParameter(String name, String description)
    {
        ParameterDeclaration parameter = new ParameterDeclaration();
        parameter.setName(name);
        parameter.setRequired(false);
        parameter.setDynamic(true);
        parameter.setType(DataType.of(String.class));
        parameter.setDescription(description);

        return parameter;
    }
}
