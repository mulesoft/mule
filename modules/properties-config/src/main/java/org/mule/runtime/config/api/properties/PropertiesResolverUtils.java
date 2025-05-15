/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.mule.runtime.properties.internal.loader.ConfigurationPropertiesProviderFactoryLoader.loadConfigurationPropertiesProviderFactories;

import static java.lang.Boolean.getBoolean;
import static java.lang.Class.forName;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationProperty;
import org.mule.runtime.core.internal.execution.LocationExecutionContextProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import org.slf4j.Logger;

/**
 * Utils for Properties Resolver Creation.
 *
 * @since 4.5
 */
public class PropertiesResolverUtils {

  /**
   * This is intended to address some situations in the Mule Runtime testing, not to use in production.
   */
  private static final String DUPLICATE_PROVIDERS_LAX = PropertiesResolverUtils.class.getName() + ".duplicateProvidersLax";

  public static final String GLOBAL_PROPERTY = "global-property";
  private static final Logger LOGGER = getLogger(PropertiesResolverUtils.class);

  private static final Class<?> PROVIDER_FACTORY_IFACE_OLD;

  static {
    final String oldInterfaceName = "org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory";

    Class<?> providerFactoryIfaceOld = null;
    try {
      providerFactoryIfaceOld = forName(oldInterfaceName);
    } catch (ClassNotFoundException e) {
      LOGGER.debug("Interface '" + oldInterfaceName + "' not available in classpath, skipping its processing.");
    }
    PROVIDER_FACTORY_IFACE_OLD = providerFactoryIfaceOld;
  }

  private PropertiesResolverUtils() {
    // do nothing
  }

  /**
   * @param artifactAst the Artifact AST to calculate the global properties from
   * @return a Lazy Evaluator to get the Global/Default Properties of a given {@link ArtifactAst}.
   */
  public static Supplier<Map<String, ConfigurationProperty>> createGlobalPropertiesSupplier(ArtifactAst artifactAst) {
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
   * @param artifactAst                 the {@link ArtifactAst} to get the {@link ConfigurationPropertiesProvider} from.
   * @param externalResourceClassLoader a {@link ClassLoader} to use to read files when needed for properties resolution.
   * @param localResolver               A resolver that retrieves properties that are used when resolving parameters of a
   *                                    {@link ConfigurationPropertiesProvider}.
   * @return A List with all the {@link ConfigurationPropertiesProvider} for Application Properties providers within the
   *         {@link ArtifactAst}.
   */
  public static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                        ClassLoader externalResourceClassLoader,
                                                                                                        ConfigurationPropertiesResolver localResolver) {
    return getConfigurationPropertiesProvidersFromComponents(artifactAst,
                                                             new ClassLoaderResourceProvider(externalResourceClassLoader),
                                                             localResolver);
  }

  /**
   * @param artifactAst                    the {@link ArtifactAst} to get the {@link ConfigurationPropertiesProvider} from.
   * @param externalResourceClassLoader    a {@link ClassLoader} to use to read files when needed for properties resolution.
   * @param localResolver                  A resolver that retrieves properties that are used when resolving parameters of a
   *                                       {@link ConfigurationPropertiesProvider}.
   * @param ignoreCreateProviderExceptions if {@code true}, exceptions that occur when calling
   *                                       {@link ConfigurationPropertiesProviderFactory#createProvider(org.mule.runtime.ast.api.ComponentAst, java.util.function.UnaryOperator, ResourceProvider)}
   *                                       are just logged instead of rethrown.
   * @return A List with all the {@link ConfigurationPropertiesProvider} for Application Properties providers within the
   *         {@link ArtifactAst}.
   *
   * @since 4.8
   */
  public static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                        ClassLoader externalResourceClassLoader,
                                                                                                        ConfigurationPropertiesResolver localResolver,
                                                                                                        boolean ignoreCreateProviderExceptions) {
    return getConfigurationPropertiesProvidersFromComponents(artifactAst,
                                                             new ClassLoaderResourceProvider(externalResourceClassLoader),
                                                             localResolver,
                                                             ignoreCreateProviderExceptions);
  }

  /**
   * @param artifactAst              the {@link ArtifactAst} to get the {@link ConfigurationPropertiesProvider} from.
   * @param externalResourceProvider a {@link ResourceProvider} to use to read files when needed for properties resolution.
   * @param localResolver            A resolver that retrieves properties that are used when resolving parameters of a
   *                                 {@link ConfigurationPropertiesProvider}.
   * @return A List with all the {@link ConfigurationPropertiesProvider} for Application Properties providers within the
   *         {@link ArtifactAst}.
   */
  public static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                        ResourceProvider externalResourceProvider,
                                                                                                        ConfigurationPropertiesResolver localResolver) {
    return getConfigurationPropertiesProvidersFromComponents(artifactAst, externalResourceProvider, localResolver, false);
  }

  /**
   * @param artifactAst                    the {@link ArtifactAst} to get the {@link ConfigurationPropertiesProvider} from.
   * @param externalResourceProvider       a {@link ResourceProvider} to use to read files when needed for properties resolution.
   * @param localResolver                  A resolver that retrieves properties that are used when resolving parameters of a
   *                                       {@link ConfigurationPropertiesProvider}.
   * @param ignoreCreateProviderExceptions if {@code true}, exceptions that occur when calling
   *                                       {@link ConfigurationPropertiesProviderFactory#createProvider(org.mule.runtime.ast.api.ComponentAst, java.util.function.UnaryOperator, ResourceProvider)}
   *                                       are just logged instead of rethrown.
   * @return A List with all the {@link ConfigurationPropertiesProvider} for Application Properties providers within the
   *         {@link ArtifactAst}.
   * @since 4.8
   */
  public static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                        ResourceProvider externalResourceProvider,
                                                                                                        ConfigurationPropertiesResolver localResolver,
                                                                                                        boolean ignoreCreateProviderExceptions) {
    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = loadProviderFactories();

    return artifactAst.topLevelComponentsStream()
        .filter(comp -> providerFactoriesMap.containsKey(comp.getIdentifier()))
        .map(comp -> {
          try {
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
            return of(provider);
          } catch (Exception e) {
            if (ignoreCreateProviderExceptions) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.warn("Exception creating property provider for component `" + comp.toString() + "`", e);
              } else {
                LOGGER.warn("Exception creating property provider for component `{}`: {}", comp, e);
              }
              return Optional.<ConfigurationPropertiesProvider>empty();
            } else {
              throw e;
            }
          }
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  /**
   * @return a {@link Map} with the {@link ConfigurationPropertiesProviderFactory} loaded using the {@link ServiceLoader}
   * @throws {@link MuleRuntimeException} if there are more than one factory for the same {@link ComponentIdentifier}.
   */
  public static Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> loadProviderFactories() {
    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = new HashMap<>();

    loadConfigurationPropertiesProviderFactories(currentThread().getContextClassLoader())
        // Skip loading these, since they will be loading by the following block
        .filter(service -> PROVIDER_FACTORY_IFACE_OLD == null || !PROVIDER_FACTORY_IFACE_OLD.isAssignableFrom(service.getClass()))
        .forEach(service -> {
          ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
          if (providerFactoriesMap.containsKey(componentIdentifier)) {
            final ConfigurationPropertiesProviderFactory previous = providerFactoriesMap.get(componentIdentifier);
            if (!getBoolean(DUPLICATE_PROVIDERS_LAX)) {
              throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component `"
                  + componentIdentifier + "`: `" + previous.toString() + "` loaded by `" + previous.getClass().getClassLoader()
                  + "` and `" + service.toString() + "` loaded by `" + service.getClass().getClassLoader() + "`."));
            }
          } else {
            providerFactoriesMap.put(componentIdentifier, service);
          }
        });

    // Support of the old deprecated interface only if it is available in the classpath.
    // This may happen only on environments where the runtime modules are uses as libs in some tool, but not when inside the
    // Runtime.
    if (PROVIDER_FACTORY_IFACE_OLD != null) {
      try {
        PROVIDER_FACTORY_IFACE_OLD
            .getMethod("loadDeprecatedProviderFactories", Map.class)
            .invoke(null, providerFactoriesMap);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
          | SecurityException e) {
        throw new MuleRuntimeException(e);
      }
    }

    return providerFactoriesMap;
  }
}
