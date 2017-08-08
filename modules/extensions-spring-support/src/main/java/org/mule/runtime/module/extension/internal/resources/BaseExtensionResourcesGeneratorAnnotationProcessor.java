/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


/**
 * Annotation processor that picks up all the extensions annotated with {@link Extension} and use a
 * {@link ResourcesGenerator} to generated the required resources.
 * <p>
 * This annotation processor will automatically generate and package into the output jar the XSD schema, spring bundles and
 * extension registration files necessary for mule to work with this extension.
 * <p>
 * Depending on the model properties declared by each extension, some of those resources might or might not be generated
 *
 * @since 3.7.0
 */
@SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION)
public abstract class BaseExtensionResourcesGeneratorAnnotationProcessor extends AbstractProcessor {

  private static final ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();

  public static final String PROCESSING_ENVIRONMENT = "PROCESSING_ENVIRONMENT";
  public static final String EXTENSION_ELEMENT = "EXTENSION_ELEMENT";
  public static final String ROUND_ENVIRONMENT = "ROUND_ENVIRONMENT";
  public static final String EXTENSION_VERSION = "extension.version";

  private final SpiServiceRegistry serviceRegistry = new SpiServiceRegistry();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    log("Starting Resources generator for Extensions");
    ResourcesGenerator generator = new AnnotationProcessorResourceGenerator(fetchResourceFactories(), processingEnv);

    try {
      getExtension(roundEnv).ifPresent(extensionElement -> {
        Optional<Class<Object>> annotatedClass = processor.classFor(extensionElement, processingEnv);
        if (!annotatedClass.isPresent()) {
          log("Extension class " + processor.getClassName(extensionElement, processingEnv) + " could not be found. Skipping");
          return;
        }
        final Class<?> extensionClass = annotatedClass.get();
        withContextClassLoader(extensionClass.getClassLoader(), () -> {
          ExtensionModel extensionModel = parseExtension(extensionElement, roundEnv);
          generator.generateFor(extensionModel);
        });
      });

      return false;
    } catch (Exception e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, format("%s\n%s", e.getMessage(), getStackTrace(e)));
      throw e;
    }
  }

  private ExtensionModel parseExtension(TypeElement extensionElement, RoundEnvironment roundEnvironment) {
    Class<?> extensionClass = processor.classFor(extensionElement, processingEnv).get();

    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, extensionClass.getName());
    params.put(VERSION, getVersion(extensionElement.getQualifiedName()));
    params.put(EXTENSION_ELEMENT, extensionElement);
    params.put(PROCESSING_ENVIRONMENT, processingEnv);
    params.put(ROUND_ENVIRONMENT, roundEnvironment);
    return getExtensionModelLoader().loadExtensionModel(extensionClass.getClassLoader(), getDefault(emptySet()), params);
  }

  private Optional<TypeElement> getExtension(RoundEnvironment env) {
    Set<TypeElement> elements = processor.getTypeElementsAnnotatedWith(Extension.class, env);
    if (elements.size() > 1) {
      String message =
          format("Only one extension is allowed per plugin, however several classes annotated with @%s were found. Offending classes are [%s]",
                 Extension.class.getSimpleName(),
                 Joiner.on(", ").join(elements.stream().map(TypeElement::getQualifiedName).collect(toList())));

      throw new RuntimeException(message);
    }

    return elements.stream().findFirst();
  }

  private void log(String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
  }

  private String getVersion(Name qualifiedName) {
    String extensionVersion = processingEnv.getOptions().get(EXTENSION_VERSION);
    if (extensionVersion == null) {
      throw new RuntimeException(String.format("Cannot resolve version for extension %s: option '%s' is missing.", qualifiedName,
                                               EXTENSION_VERSION));
    }

    return extensionVersion;
  }

  private List<GeneratedResourceFactory> fetchResourceFactories() {

    return ImmutableList.<GeneratedResourceFactory>builder()
        .addAll(serviceRegistry.lookupProviders(GeneratedResourceFactory.class, getClass().getClassLoader()))
        .addAll(serviceRegistry.lookupProviders(DslResourceFactory.class, getClass().getClassLoader())).build();

  }

  protected abstract ExtensionModelLoader getExtensionModelLoader();
}
