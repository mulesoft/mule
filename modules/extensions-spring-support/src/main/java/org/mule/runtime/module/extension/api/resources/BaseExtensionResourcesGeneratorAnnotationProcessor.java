/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.resources;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.extension.privileged.spi.ExtensionsApiSpiUtils.loadDslResourceFactories;
import static org.mule.runtime.extension.privileged.spi.ExtensionsApiSpiUtils.loadGeneratedResourceFactories;
import static org.mule.runtime.module.artifact.activation.api.extension.discovery.boot.ExtensionLoaderUtils.getLoaderById;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator.EXPORTED_PACKAGES_VALIDATOR_SKIP;
import static org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator.EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static javax.lang.model.SourceVersion.RELEASE_17;
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;
import org.mule.runtime.module.extension.internal.resources.AnnotationProcessorProblemsHandler;
import org.mule.runtime.module.extension.internal.resources.AnnotationProcessorResourceGenerator;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.common.base.Joiner;

/**
 * Annotation processor that picks up all the extensions annotated with {@link Extension} or
 * {@link org.mule.sdk.api.annotation.Extension} and uses a {@link ResourcesGenerator} to generate the required resources.
 * <p>
 * This annotation processor will automatically generate and package into the output jar the XSD schema, spring bundles and
 * extension registration files necessary for mule to work with this extension.
 * <p>
 * Depending on the model properties declared by each extension, some of those resources might or might not be generated
 *
 * @since 3.7.0
 */
@SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
@SupportedSourceVersion(RELEASE_17)
@SupportedOptions({
    BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION,
    BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_RESOURCES,
    BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_CLASSES,
    EXPORTED_PACKAGES_VALIDATOR_SKIP,
    EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION
})
public abstract class BaseExtensionResourcesGeneratorAnnotationProcessor extends AbstractProcessor {

  static final ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();

  public static final String PROCESSING_ENVIRONMENT = "PROCESSING_ENVIRONMENT";
  public static final String EXTENSION_ELEMENT = "EXTENSION_ELEMENT";
  public static final String ROUND_ENVIRONMENT = "ROUND_ENVIRONMENT";
  public static final String PROBLEMS_HANDLER = "PROBLEMS_HANDLER";
  public static final String EXTENSION_VERSION = "extension.version";
  public static final String EXTENSION_RESOURCES = "extension.resources";
  public static final String EXTENSION_CLASSES = "extension.classes";
  public static final String EXTENSION_TYPE = "EXTENSION_TYPE";

  private static final String EXTENSION_LOADING_MODE_SYSTEM_PROPERTY = "modelLoader.runtimeMode";
  public static final String COMPILATION_MODE = "COMPILATION_MODE";

  private final LazyValue<ExtensionModelLoader> javaExtensionModelLoader = new LazyValue<>(() -> getLoaderById("java"));

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    log("Starting Resources generator for Extensions");

    ResourcesGenerator generator = new AnnotationProcessorResourceGenerator(fetchResourceFactories(), processingEnv);

    try {
      getExtension(roundEnv).ifPresent(extensionElement -> {
        if (!shouldProcess(extensionElement, processingEnv)) {
          return;
        }
        ExtensionElement extension = toExtensionElement(extensionElement, processingEnv);

        ClassLoader classLoader;
        final String extensionResourcesLocation = processingEnv.getOptions().get(EXTENSION_RESOURCES);
        if (extensionResourcesLocation != null) {
          // make sure the static resource files from the extension are available through the TCCL, even if not available through
          // the processor CL.
          classLoader = createClassloaderWithExtensionResources(extensionResourcesLocation, processor.getExtensionClassLoader());
        } else {
          classLoader = processor.getExtensionClassLoader();
        }

        withContextClassLoader(classLoader, () -> {
          ExtensionModel extensionModel =
              parseExtension(extensionElement, extension, roundEnv, currentThread().getContextClassLoader());
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

  private ClassLoader createClassloaderWithExtensionResources(String extensionResourcesLocation, ClassLoader parentClassLoader) {
    ClassLoader classLoaderWithExtensionResources;
    try {
      classLoaderWithExtensionResources =
          new URLClassLoader(new URL[] {Paths.get(extensionResourcesLocation).toUri().toURL()},
                             parentClassLoader);
    } catch (MalformedURLException e) {
      processingEnv.getMessager().printMessage(ERROR, format("%s\n%s", e.getMessage(), getStackTrace(e)));
      throw new RuntimeException(e);
    }
    return classLoaderWithExtensionResources;
  }

  /**
   * @return the {@link ExtensionModelLoader} for loading Java based Extensions
   */
  protected ExtensionModelLoader fetchJavaExtensionModelLoader() {
    return javaExtensionModelLoader.get();
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

    if (!simulateRuntimeLoading()) {
      // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
      params.put(COMPILATION_MODE, true);
    }

    final var builder = builder(classLoader, getDefault(singleton(MuleExtensionModelProvider.getExtensionModel())));
    configureLoadingRequest(builder);
    return getExtensionModelLoader()
        .loadExtensionModel(builder
            .addParameters(params)
            .setForceExtensionValidation(true)
            .build());
  }

  private Optional<TypeElement> getExtension(RoundEnvironment env) {
    Set<TypeElement> elements = processor.getTypeElementsAnnotatedWith(Extension.class, env);
    elements.addAll(processor.getTypeElementsAnnotatedWith(org.mule.sdk.api.annotation.Extension.class, env));

    if (elements.size() > 1) {
      String message =
          format("Only one extension is allowed per plugin, however several classes annotated with either @%s or @%s were found. Offending classes are [%s]",
                 Extension.class.getName(),
                 org.mule.sdk.api.annotation.Extension.class.getName(),
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
    return unmodifiableList(concat(loadGeneratedResourceFactories(),
                                   loadDslResourceFactories())
        .collect(toList()));
  }

  /**
   * During compile-time, some model validations will be performed over the plugin being compiled that are different from the ones
   * executed at execution-time for the same plugin (being the runtime validations a subset of the ones executed at compile-time).
   * <p>
   * Ir order to skip the compile-time-only validations and load the plugin as if it was loaded on an application deploy, the user
   * can flag the compilation as a "runtime simulation". For example, a plugin that has been developed using a 1.0 version of the
   * SDK and fails its compilation when moving to the 1.1 version of the SDK, should never fail when using the "runtime
   * simulation" loading mode (otherwise runtime backwards compatibility would've been broken).
   * <p>
   * This simulation mode should be treated as an internal, test-only configuration.
   *
   * @return {@code true} if {@code modelLoader.runtimeMode} configuration property was provided
   */
  private boolean simulateRuntimeLoading() {
    String runtimeMode = System.getProperty(EXTENSION_LOADING_MODE_SYSTEM_PROPERTY);
    return runtimeMode != null && !runtimeMode.trim().isEmpty() ? Boolean.valueOf(runtimeMode) : false;
  }

  public abstract ExtensionElement toExtensionElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment);

  protected abstract ExtensionModelLoader getExtensionModelLoader();

  /**
   * Override this method for the chance of adding custom parameterization into the {@code requestBuilder}.
   * <p>
   * The same builder will later be used to create the {@link ExtensionModelLoadingRequest} used in the
   * {@link ExtensionModelLoader#loadExtensionModel(ExtensionModelLoadingRequest)} invocation.
   * <p>
   * This default implementation is no-op
   *
   * @param requestBuilder a {@link ExtensionModelLoadingRequest.Builder}
   * @since 4.5.0
   */
  protected void configureLoadingRequest(ExtensionModelLoadingRequest.Builder requestBuilder) {
    // no-op
  }

  /**
   * @return a boolean indicating if the annotation processor is able to process or not with the current context.
   */
  protected abstract boolean shouldProcess(TypeElement extensionElement, ProcessingEnvironment processingEnv);

}
