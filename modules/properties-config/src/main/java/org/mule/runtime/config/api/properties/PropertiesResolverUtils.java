/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import static org.mule.runtime.api.component.AbstractComponent.ANNOTATION_NAME;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.Class.forName;
import static java.lang.String.format;
import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationProperty;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;
import org.slf4j.Logger;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Utils for Properties Resolver Creations.
 * 
 * @since 4.5
 */
public class PropertiesResolverUtils {

  public static final String GLOBAL_PROPERTY = "global-property";
  private static final Logger LOGGER = getLogger(PropertiesResolverUtils.class);

  private PropertiesResolverUtils() {
    // do nothing
  }

  /**
   * @param artifactAst the Artifact AST to calculate the global properties from
   * @return a Lazy Evaluator to get the Global/Default Properties of a given {@link ArtifactAst}.
   */
  public static Supplier<Map<String, org.mule.runtime.properties.api.ConfigurationProperty>> createGlobalPropertiesSupplier(ArtifactAst artifactAst) {
    return new LazyValue<>(() -> {
      final Map<String, ConfigurationProperty> globalProperties = new HashMap<>();

      artifactAst.topLevelComponentsStream()
          .filter(comp -> GLOBAL_PROPERTY.equals(comp.getIdentifier().getName()))
          .forEach(comp -> {
            final String key = comp.getParameter(DEFAULT_GROUP_NAME, "name").getResolvedRawValue();
            final String rawValue = comp.getParameter(DEFAULT_GROUP_NAME, "value").getRawValue();
            globalProperties.put(key,
                                 new DefaultConfigurationProperty(format("global-property - file: %s - lineNumber %s",
                                                                         comp.getMetadata().getFileName().orElse("(n/a)"),
                                                                         comp.getMetadata().getStartLine().orElse(-1)),
                                                                  key, rawValue));
          });

      return globalProperties;
    });
  }

  /**
   * @param artifactAst
   * @param externalResourceProvider
   * @param localResolver            A resolver that retrieves properties that are used in
   * @return A List with all the {@link ConfigurationPropertiesProvider} for Application Properties providers
   */
  public static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                        ResourceProvider externalResourceProvider,
                                                                                                        ConfigurationPropertiesResolver localResolver) {
    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = loadProviderFactories();

    return artifactAst.topLevelComponentsStream()
        .filter(comp -> providerFactoriesMap.containsKey(comp.getIdentifier()))
        .map(comp -> {
          ConfigurationPropertiesProvider provider = providerFactoriesMap.get(comp.getIdentifier())
              .createProvider(comp, localResolver, externalResourceProvider);
          if (provider instanceof Component) {
            final Map<QName, Object> annotations = new HashMap<>();
            annotations.put(LOCATION_KEY, comp.getLocation());
            annotations.put(ANNOTATION_NAME, comp.getIdentifier());
            annotations.put(SOURCE_ELEMENT_ANNOTATION_KEY,
                            comp.getMetadata().getSourceCode()
                                .map(LocationExecutionContextProvider::maskPasswords)
                                .orElse(null));

            ((Component) provider).setAnnotations(annotations);
          }
          return provider;
        })
        .collect(toList());
  }

  /**
   *
   * @return
   */
  public static Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> loadProviderFactories() {
    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = new HashMap<>();

    ServiceLoader<ConfigurationPropertiesProviderFactory> providerFactories = load(ConfigurationPropertiesProviderFactory.class);
    providerFactories.forEach(service -> {
      ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
      if (providerFactoriesMap.containsKey(componentIdentifier)) {
        throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component: "
            + componentIdentifier));
      }
      providerFactoriesMap.put(componentIdentifier, service);
    });

    // Support of the old deprecated interface only if it is available in the classpath.
    // This may happen only on environments where the runtime modules are uses as libs in some tool, but not when inside the
    // Runtime.
    try {
      Class<?> providerFactoryIfaceOld =
          forName("org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory");

      ServiceLoader<? extends ConfigurationPropertiesProviderFactory> providerFactoriesOld =
          (ServiceLoader<? extends ConfigurationPropertiesProviderFactory>) load(providerFactoryIfaceOld);
      providerFactoriesOld.forEach(service -> {
        ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
        if (providerFactoriesMap.containsKey(componentIdentifier)) {
          throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component: "
              + componentIdentifier));
        }
        providerFactoriesMap.put(componentIdentifier, service);
      });
    } catch (ClassNotFoundException e) {
      LOGGER
          .debug("Interface 'org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory' not available in classpath, skipping its processing.");
    }

    return providerFactoriesMap;
  }
}
