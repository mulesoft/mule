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
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
  public static final String PROBLEMS_HANDLER = "PROBLEMS_HANDLER";
  public static final String EXTENSION_VERSION = "extension.version";
  public static final String EXTENSION_TYPE = "EXTENSION_TYPE";

  private final SpiServiceRegistry serviceRegistry = new SpiServiceRegistry();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    log("Starting Resources generator for Extensions");
    ResourcesGenerator generator = new AnnotationProcessorResourceGenerator(fetchResourceFactories(), processingEnv);

    try {
      getExtension(roundEnv).ifPresent(extensionElement -> {
        Optional<Class<Object>> annotatedClass = processor.classFor(extensionElement, processingEnv);
        ExtensionElement extension = toExtensionElement(extensionElement, processingEnv);
        ClassLoader classLoader = annotatedClass.map(Class::getClassLoader).orElseGet(ExtensionModel.class::getClassLoader);
        withContextClassLoader(classLoader, () -> {
          ExtensionModel extensionModel = parseExtension(extensionElement, extension, roundEnv, classLoader);
          generator.generateFor(extensionModel);
        });
      });

      return false;
    } catch (MuleRuntimeException e) {
      Optional<IllegalModelDefinitionException> exception = extractOfType(e, IllegalModelDefinitionException.class);
      if (exception.isPresent()) {
        throw exception.get();
      }

      processingEnv.getMessager().printMessage(ERROR, format("%s\n%s", e.getMessage(), getStackTrace(e)));
      throw e;
    }
  }

  private ExtensionModel parseExtension(TypeElement extensionElement, ExtensionElement extension,
                                        RoundEnvironment roundEnvironment, ClassLoader classLoader) {

    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, extensionElement.toString());
    params.put(VERSION, getVersion(extensionElement.getQualifiedName()));
    params.put(EXTENSION_TYPE, extension);
    params.put(EXTENSION_ELEMENT, extensionElement);
    params.put(PROBLEMS_HANDLER, new AnnotationProcessorProblemsHandler(processingEnv));
    params.put(PROCESSING_ENVIRONMENT, processingEnv);
    params.put(ROUND_ENVIRONMENT, roundEnvironment);
    return getExtensionModelLoader().loadExtensionModel(classLoader, getDefault(emptySet()),
                                                        params);
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

  public abstract ExtensionElement toExtensionElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment);

  protected abstract ExtensionModelLoader getExtensionModelLoader();
}
