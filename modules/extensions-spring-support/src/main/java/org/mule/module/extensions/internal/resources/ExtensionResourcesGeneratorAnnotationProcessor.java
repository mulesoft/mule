/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;

import static org.mule.util.Preconditions.checkState;
import org.mule.api.registry.SPIServiceRegistry;
import org.mule.extensions.introspection.Describer;
import org.mule.extensions.introspection.DescribingContext;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.ExtensionFactory;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.resources.ResourcesGenerator;
import org.mule.module.extensions.internal.ImmutableDescribingContext;
import org.mule.module.extensions.internal.capability.xml.XmlCapabilityExtractor;
import org.mule.module.extensions.internal.capability.xml.schema.SchemaDocumenterPostProcessor;
import org.mule.module.extensions.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extensions.internal.introspection.DefaultExtensionFactory;
import org.mule.util.ClassUtils;
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
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Annotation processor that picks up all the extensions annotated with
 * {@link Extension} and use a
 * {@link ResourcesGenerator} to generated
 * the required resources.
 * <p/>
 * This annotation processor will automatically generate and package into the output jar
 * the XSD schema, spring bundles and extension registration files
 * necessary for mule to work with this extension.
 * <p/>
 * Depending on the capabilities declared by each extension, some of those resources
 * might or might not be generated
 *
 * @since 3.7.0
 */
@SupportedAnnotationTypes(value = {"org.mule.extensions.api.annotation.Extension"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ExtensionResourcesGeneratorAnnotationProcessor extends AbstractProcessor
{
    private final ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SPIServiceRegistry());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        log("Starting Resources generator for Extensions");

        ResourcesGenerator generator = new AnnotationProcessorResourceGenerator(processingEnv, new SPIServiceRegistry());
        try
        {
            for (TypeElement extensionElement : findExtensions(roundEnv))
            {
                Extension extension = parseExtension(extensionElement);
                generator.generateFor(extension);
            }

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

    private Extension parseExtension(TypeElement extensionElement)
    {
        Class<?> extensionClass = getClass(extensionElement);
        Describer describer = new AnnotationsBasedDescriber(extensionClass);

        DescribingContext context = new ImmutableDescribingContext(describer.describe().getRootConstruct());
        context.getCustomParameters().put(SchemaDocumenterPostProcessor.EXTENSION_ELEMENT, extensionElement);
        context.getCustomParameters().put(SchemaDocumenterPostProcessor.PROCESSING_ENVIRONMENT, processingEnv);

        extractXmlCapability(extensionClass, context);

        return extensionFactory.createFrom(context.getDeclarationConstruct());
    }

    private XmlCapability extractXmlCapability(Class<?> extensionClass, DescribingContext context)
    {
        XmlCapabilityExtractor extractor = new XmlCapabilityExtractor();
        XmlCapability capability = (XmlCapability) extractor.extractCapability(context.getDeclarationConstruct(), extensionClass, context.getDeclarationConstruct());
        checkState(capability != null, "Could not find xml capability for extension " + extensionClass.getName());

        return capability;
    }


    private List<TypeElement> findExtensions(RoundEnvironment env)
    {
        return ImmutableList.copyOf(ElementFilter.typesIn(env.getElementsAnnotatedWith(org.mule.extensions.annotations.Extension.class)));
    }

    private void log(String message)
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }


    private Class<?> getClass(TypeElement element)
    {
        final String classname = element.getQualifiedName().toString();
        try
        {
            ClassUtils.loadClass(classname, getClass());
            return ClassUtils.getClass(getClass().getClassLoader(), classname, true);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(
                    String.format("Could not load class %s while trying to generate XML schema", classname), e);
        }
    }
}
