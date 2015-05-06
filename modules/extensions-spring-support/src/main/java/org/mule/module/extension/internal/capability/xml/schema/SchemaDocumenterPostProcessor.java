/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema;

import static org.mule.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_ELEMENT;
import static org.mule.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.PROCESSING_ENVIRONMENT;
import static org.mule.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.ROUND_ENVIRONMENT;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.introspection.DescribingContext;
import org.mule.extension.introspection.spi.DescriberPostProcessor;
import org.mule.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
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


    @Override
    public void postProcess(DescribingContext context)
    {
        ProcessingEnvironment processingEnv = getCheckedParameter(context, PROCESSING_ENVIRONMENT, ProcessingEnvironment.class);
        TypeElement extensionElement = getCheckedParameter(context, EXTENSION_ELEMENT, TypeElement.class);
        RoundEnvironment roundEnvironment = getCheckedParameter(context, ROUND_ENVIRONMENT, RoundEnvironment.class);

        if (processingEnv == null || extensionElement == null)
        {
            logger.debug("processing environment or extension element not provided. Skipping");
            return;
        }

        new SchemaDocumenter(processingEnv).document(context.getDeclarationConstruct().getDeclaration(), extensionElement, roundEnvironment);
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
