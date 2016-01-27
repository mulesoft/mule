/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.resources;

import static org.mule.module.extension.internal.capability.xml.schema.AnnotationProcessorUtils.getTypeElementsAnnotatedWith;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.extension.api.resources.ResourcesGenerator;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.capability.xml.schema.AnnotationProcessorUtils;
import org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.module.extension.internal.introspection.VersionResolver;
import org.mule.registry.SpiServiceRegistry;
import org.mule.util.ExceptionUtils;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Annotation processor that picks up all the extensions annotated with
 * {@link ExtensionModel} and use a
 * {@link ResourcesGenerator} to generated
 * the required resources.
 * <p/>
 * This annotation processor will automatically generate and package into the output jar
 * the XSD schema, spring bundles and extension registration files
 * necessary for mule to work with this extension.
 * <p/>
 * Depending on the model properties declared by each extension, some of those resources
 * might or might not be generated
 *
 * @since 3.7.0
 */
@SupportedAnnotationTypes(value = {"org.mule.extension.annotation.api.Extension"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ExtensionResourcesGeneratorAnnotationProcessor extends AbstractProcessor
{

    public static final String PROCESSING_ENVIRONMENT = "PROCESSING_ENVIRONMENT";
    public static final String EXTENSION_ELEMENT = "EXTENSION_ELEMENT";
    public static final String ROUND_ENVIRONMENT = "ROUND_ENVIRONMENT";

    private final SpiServiceRegistry serviceRegistry = new SpiServiceRegistry();
    private final ExtensionFactory extensionFactory = new DefaultExtensionFactory(serviceRegistry, getClass().getClassLoader());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        log("Starting Resources generator for Extensions");
        ResourcesGenerator generator = new AnnotationProcessorResourceGenerator(processingEnv, new SpiServiceRegistry());
        try
        {
            findExtensions(roundEnv).forEach(extensionElement -> {
                ExtensionModel extensionModel = parseExtension(extensionElement, roundEnv);
                generator.generateFor(extensionModel);
            });

            generator.dumpAll();

            return false;
        }
        catch (Exception e)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     String.format("%s\n%s", e.getMessage(), ExceptionUtils.getFullStackTrace(e)));
            throw e;
        }
    }

    private ExtensionModel parseExtension(TypeElement extensionElement, RoundEnvironment roundEnvironment)
    {
        Class<?> extensionClass = AnnotationProcessorUtils.classFor(extensionElement, processingEnv);
        Describer describer = new AnnotationsBasedDescriber(extensionClass, new FixedVersionResolver());

        DescribingContext context = new DefaultDescribingContext();
        context.addParameter(EXTENSION_ELEMENT, extensionElement);
        context.addParameter(PROCESSING_ENVIRONMENT, processingEnv);
        context.addParameter(ROUND_ENVIRONMENT, roundEnvironment);

        return extensionFactory.createFrom(describer.describe(context), context);
    }

    private List<TypeElement> findExtensions(RoundEnvironment env)
    {
        return ImmutableList.copyOf(getTypeElementsAnnotatedWith(Extension.class, env));
    }

    private void log(String message)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    private class FixedVersionResolver implements VersionResolver
    {

        @Override
        public String resolveVersion(Extension extension)
        {
            String extensionVersion = processingEnv.getOptions().get("extension.version");
            if (extensionVersion == null)
            {
                throw new RuntimeException(String.format("Cannot resolve version for extension %s: option extension.version is missing.", extension.name()));
            }
            return extensionVersion;
        }
    }
}
