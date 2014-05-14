/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml.schema;

import static org.mule.module.extensions.internal.capability.xml.schema.AnnotationProcessorUtils.getFieldsAnnotatedWith;
import static org.mule.module.extensions.internal.capability.xml.schema.AnnotationProcessorUtils.getJavaDocSummary;
import static org.mule.module.extensions.internal.capability.xml.schema.AnnotationProcessorUtils.getMethodDocumentation;
import static org.mule.module.extensions.internal.capability.xml.schema.AnnotationProcessorUtils.getMethodsAnnotatedWith;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.extensions.annotations.Operation;
import org.mule.extensions.annotations.Parameter;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.declaration.ConfigurationDeclaration;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.extensions.introspection.declaration.OperationDeclaration;
import org.mule.extensions.introspection.declaration.ParameterDeclaration;

import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Utility class that picks a {@link Declaration}
 * on which a {@link Extension} has already been described
 * and enriches such description with the javadocs extracted from the extension's acting classes.
 * <p/>
 * This is necessary because such documentation is not available on runtime, thus this class
 * uses the annotation processor's AST access to extract it
 *
 * @since 3.7.0
 */
final class SchemaDocumenter
{

    private ProcessingEnvironment processingEnv;

    SchemaDocumenter(ProcessingEnvironment processingEnv)
    {
        this.processingEnv = processingEnv;
    }

    void document(Declaration declaration, TypeElement extensionElement)
    {
        declaration.setDescription(getJavaDocSummary(processingEnv, extensionElement));
        documentConfigurations(declaration, extensionElement);
        documentOperations(declaration, extensionElement);
    }

    private void documentOperations(Declaration declaration, TypeElement extensionElement)
    {
        final Map<String, ExecutableElement> methods = getMethodsAnnotatedWith(extensionElement, Operation.class);

        try
        {
            for (OperationDeclaration operation : declaration.getOperations())
            {
                ExecutableElement method = methods.get(operation.getName());

                if (method == null)
                {
                    continue;
                }

                MethodDocumentation documentation = getMethodDocumentation(processingEnv, method);
                operation.setDescription(documentation.getSummary());
                documentOperationParameters(operation, documentation);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Exception found while trying to document XSD schema"), e);
        }
    }

    private void documentOperationParameters(OperationDeclaration operation, MethodDocumentation documentation)
    {
        for (ParameterDeclaration parameter : operation.getParameters())
        {
            String description = documentation.getParameters().get(parameter.getName());
            if (description != null)
            {
                parameter.setDescription(description);
            }
        }
    }

    private void documentConfigurations(Declaration declaration, TypeElement extensionElement)
    {
        for (ConfigurationDeclaration configuration : declaration.getConfigurations())
        {
            documentConfigurationParameters(configuration.getParameters(), extensionElement);
        }
    }

    private void documentConfigurationParameters(Collection<ParameterDeclaration> parameters, TypeElement element)
    {
        final Map<String, VariableElement> fields = getFieldsAnnotatedWith(element, Parameter.class);
        while (element != null && !Object.class.getName().equals(element.getQualifiedName().toString()))
        {
            for (ParameterDeclaration parameter : parameters)
            {
                VariableElement field = fields.get(parameter.getName());
                if (field != null)
                {
                    parameter.setDescription(getJavaDocSummary(processingEnv, field));
                }
            }

            element = (TypeElement) processingEnv.getTypeUtils().asElement(element.getSuperclass());
        }
    }
}
