/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import static org.mule.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.extension.annotation.api.ExposeContentType;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;

/**
 * Enriches operations which were defined in methods annotated with {@link ExposeContentType} so that
 * parameters related to {@link ContentType} are added. In particular, it adds two parameters named
 * {@link ExtensionProperties#MIME_TYPE_PARAMETER_NAME} and {@link ExtensionProperties#ENCODING_PARAMETER_NAME}.
 * <p>
 * Both attributes are optional, have no default value and accept expressions.
 *
 * @since 4.0
 */
public class ContentTypeModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        describingContext.getDeclarationDescriptor().getDeclaration().getOperations().forEach(operation -> {
            ExposeContentType annotation = extractAnnotation(operation, ExposeContentType.class);
            if (annotation != null)
            {
                operation.addParameter(newParameter(MIME_TYPE_PARAMETER_NAME, "The mime type to be set on the message"));
                operation.addParameter(newParameter(ENCODING_PARAMETER_NAME, "The encoding to be set on the message"));
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
