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
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.spi.DescriberPostProcessor;
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
 * This post processor uses the APT API to access the AST tree and extract the extensions
 * javadocs which are used to enrich the extension's descriptions.
 * <p/>
 * For this to be possible, the context should have as custom parameters a {@link ProcessingEnvironment}
 * and the corresponding {@link TypeElement}, which will be fetched in the
 * provided context under the keys {@link ExtensionResourcesGeneratorAnnotationProcessor#PROCESSING_ENVIRONMENT} and
 * {@link ExtensionResourcesGeneratorAnnotationProcessor#EXTENSION_ELEMENT}.
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
        ProcessingEnvironment processingEnv = context.getCheckedParameter(PROCESSING_ENVIRONMENT, ProcessingEnvironment.class);
        TypeElement extensionElement = context.getCheckedParameter(EXTENSION_ELEMENT, TypeElement.class);
        RoundEnvironment roundEnvironment = context.getCheckedParameter(ROUND_ENVIRONMENT, RoundEnvironment.class);

        if (processingEnv == null || extensionElement == null)
        {
            logger.debug("processing environment or extension element not provided. Skipping");
            return;
        }

        new SchemaDocumenter(processingEnv).document(context.getDeclarationDescriptor().getDeclaration(), extensionElement, roundEnvironment);
    }
}
