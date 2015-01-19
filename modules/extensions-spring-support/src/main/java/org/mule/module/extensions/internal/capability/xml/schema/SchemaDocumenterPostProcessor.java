/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml.schema;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.DescribingContext;
import org.mule.extensions.introspection.spi.DescriberPostProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link DescriberPostProcessor}
 * that's only applicable when invoked in the context of an
 * annotations {@link javax.annotation.processing.Processor}.
 * <p/>
 * This post processor uses the APT api to access the AST tree and extract the extensions
 * javadocs which are used to enrich the extension's descriptions.
 * <p/>
 * For this to be possible, the context should have as custom parameters a {@link ProcessingEnvironment}
 * and the corresponding {@link TypeElement}, which will be fetched in the
 * provided context under the keys {@link #PROCESSING_ENVIRONMENT} and {@link #EXTENSION_ELEMENT}
 * <p/>
 * If any of the above requirements is not met, then the post processor will skip the extension
 *
 * @since 3.7.0
 */
public final class SchemaDocumenterPostProcessor implements DescriberPostProcessor
{

    private static Logger logger = LoggerFactory.getLogger(SchemaDocumenterPostProcessor.class);

    public static final String PROCESSING_ENVIRONMENT = "PROCESSING_ENVIRONMENT";
    public static final String EXTENSION_ELEMENT = "EXTENSION_ELEMENT";

    @Override
    public void postProcess(DescribingContext context)
    {
        ProcessingEnvironment processingEnv = getCheckedParameter(context, PROCESSING_ENVIRONMENT, ProcessingEnvironment.class);
        TypeElement extensionElement = getCheckedParameter(context, EXTENSION_ELEMENT, TypeElement.class);

        if (processingEnv == null || extensionElement == null)
        {
            logger.debug("processing environment or extension element not provided. Skipping");
            return;
        }

        new SchemaDocumenter(processingEnv).document(context.getDeclarationConstruct().getDeclaration(), extensionElement);
    }

    private <T> T getCheckedParameter(DescribingContext context, String key, Class<T> expectedType)
    {
        Object parameter = context.getCustomParameters().get(key);
        if (parameter == null)
        {
            return null;
        }

        checkArgument(expectedType.isInstance(parameter),
                      String.format("Custom parameter '%s' was expected to be of class '%s' but got '%s' instead",
                                    key,
                                    expectedType.getName(),
                                    parameter.getClass().getName()));

        return (T) parameter;
    }
}
