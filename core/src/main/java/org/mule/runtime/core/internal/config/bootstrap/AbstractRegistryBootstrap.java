/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.artifact.ArtifactType.APP;
import static org.mule.runtime.api.artifact.ArtifactType.DOMAIN;
import static org.mule.runtime.api.artifact.ArtifactType.POLICY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang3.exception.ExceptionUtils.getCause;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.builders.RegistryBootstrap;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.PropertiesUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link RegistryBootstrap} implementations
 *
 * @since 3.7.0
 */
public abstract class AbstractRegistryBootstrap implements RegistryBootstrap {

  static final String APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY = "applyToArtifactType";

  private static final String RETURN_CLASS_PROPERTY = "returnClass";
  private static final String MIME_TYPE_PROPERTY = "mimeType";

  public static final String TRANSFORMER_KEY = ".transformer.";
  public static final String OBJECT_KEY = ".object.";
  public static final String SINGLE_TX = ".singletx.";

  /**
   * Indicates if a propertyKey is a property that declares a transformer
   */
  public static final Predicate<String> TRANSFORMER_PREDICATE =
      propertyKey -> propertyKey.contains(TRANSFORMER_KEY);
  /**
   * Indicates if a propertyKey is a property that declares a bindingProvider to use in the expressions language
   */
  public static final Predicate<String> BINDING_PROVIDER_PREDICATE =
      propertyKey -> propertyKey.endsWith(".binding.provider") || propertyKey.endsWith("FunctionsProvider");

  protected ArtifactType artifactType = APP;
  protected final transient Logger logger = LoggerFactory.getLogger(getClass());
  private BootstrapServiceDiscoverer bootstrapServiceDiscoverer;
  private final Predicate<String> propertyKeyfilter;

  /**
   * @param artifactType               type of artifact. Bootstrap entries may be associated to an specific type of artifact. If
   *                                   it's not associated to the related artifact it will be ignored.
   * @param bootstrapServiceDiscoverer {@link BootstrapServiceDiscoverer} used to bootstrap a {@link MuleContext}
   */
  public AbstractRegistryBootstrap(ArtifactType artifactType, BootstrapServiceDiscoverer bootstrapServiceDiscoverer,
                                   Predicate<String> propertyKeyfilter) {
    this.artifactType = artifactType;
    this.bootstrapServiceDiscoverer = bootstrapServiceDiscoverer;
    this.propertyKeyfilter = propertyKeyfilter;
  }

  @Override
  public void initialise() throws InitialisationException {
    List<BootstrapService> bootstrapServices;
    try {
      bootstrapServices = bootstrapServiceDiscoverer.discover();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    // Merge and process properties
    int objectCounter = 1;
    List<TransformerBootstrapProperty> transformers = new LinkedList<>();
    List<ObjectBootstrapProperty> bindingProviders = new LinkedList<>();
    List<ObjectBootstrapProperty> namedObjects = new LinkedList<>();
    List<ObjectBootstrapProperty> unnamedObjects = new LinkedList<>();

    for (BootstrapService bootstrapService : bootstrapServices) {
      Properties bootstrapProperties = bootstrapService.getProperties();

      for (Map.Entry entry : bootstrapProperties.entrySet()) {
        logger.debug("Processing registryBootstrap entry: {}={}...", entry.getKey(), entry.getValue());

        final String propertyKey = (String) entry.getKey();
        final String propertyValue = (String) entry.getValue();

        if (!propertyKeyfilter.test(propertyKey)) {
          logger.debug("Skipped registryBootstrap entry: {}", entry.getKey());
          continue;
        }

        if (propertyKey.contains(OBJECT_KEY)) {
          logger.debug("Unnamed registryBootstrap entry: {}", entry.getKey());
          String newKey = propertyKey.substring(0, propertyKey.lastIndexOf(".")) + objectCounter++;
          unnamedObjects.add(createObjectBootstrapProperty(bootstrapService, newKey, propertyValue));
        } else if (TRANSFORMER_PREDICATE.test(propertyKey)) {
          logger.debug("Transformer registryBootstrap entry: {}", entry.getKey());
          transformers.add(createTransformerBootstrapProperty(bootstrapService, propertyValue));
        } else if (BINDING_PROVIDER_PREDICATE.test(propertyKey)) {
          logger.debug("Binding Provider registryBootstrap entry: {}", entry.getKey());
          bindingProviders.add(createObjectBootstrapProperty(bootstrapService, propertyKey, propertyValue));
        } else {
          logger.debug("Named registryBootstrap entry: {}", entry.getKey());
          namedObjects.add(createObjectBootstrapProperty(bootstrapService, propertyKey, propertyValue));
        }
      }
    }

    try {
      registerUnnamedObjects(unnamedObjects);
      registerTransformers(transformers);
      registerObjects(bindingProviders);
      registerObjects(namedObjects);
    } catch (Exception e1) {
      throw new InitialisationException(e1, this);
    }
  }

  private TransformerBootstrapProperty createTransformerBootstrapProperty(BootstrapService bootstrapService,
                                                                          String propertyValue) {
    String transString;
    String name = null;
    String returnClassName;
    transString = propertyValue;
    returnClassName = null;
    int index = transString.indexOf(",");
    if (index > -1) {
      Properties p = PropertiesUtils.getPropertiesFromString(transString.substring(index + 1), ',');
      name = p.getProperty("name", null);
      returnClassName = p.getProperty("returnClass", null);
    }
    String mime = null;

    if (returnClassName != null) {
      int i = returnClassName.indexOf(":");
      if (i > -1) {
        mime = returnClassName.substring(i + 1);
        returnClassName = returnClassName.substring(0, i);
      }
    }

    final String className = index == -1 ? transString : transString.substring(0, index);

    final Map<String, String> properties = new HashMap<>();
    properties.put(MIME_TYPE_PROPERTY, mime);
    properties.put(RETURN_CLASS_PROPERTY, returnClassName);

    return new TransformerBootstrapProperty(bootstrapService, new HashSet<>(asList(APP, POLICY)), name, className,
                                            returnClassName, mime);
  }

  private ObjectBootstrapProperty createObjectBootstrapProperty(BootstrapService bootstrapService, String propertyKey,
                                                                String propertyValue) {
    String className;
    Set<ArtifactType> artifactTypesParameterValue = new HashSet<>(asList(APP, DOMAIN, POLICY));

    final String value = propertyValue;
    int index = value.indexOf(",");
    if (index > -1) {
      Properties p = PropertiesUtils.getPropertiesFromString(value.substring(index + 1), ',');
      if (p.containsKey(APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY)) {

        artifactTypesParameterValue = stream(((String) p.get(APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY)).split("\\/"))
            .map(AbstractRegistryBootstrap::fromString)
            .collect(toSet());
      }
      className = value.substring(0, index);
    } else {
      className = value;
    }

    return new ObjectBootstrapProperty(bootstrapService, artifactTypesParameterValue, propertyKey, className);
  }

  private static ArtifactType fromString(String artifactTypeAsString) {
    for (ArtifactType artifactType : ArtifactType.values()) {
      if (artifactType.getArtifactTypeAsString().equals(artifactTypeAsString)) {
        return artifactType;
      }
    }
    throw new MuleRuntimeException(createStaticMessage("No artifact type found for value: " + artifactTypeAsString));
  }

  private void registerUnnamedObjects(List<ObjectBootstrapProperty> bootstrapProperties) throws Exception {
    for (ObjectBootstrapProperty bootstrapProperty : bootstrapProperties) {
      registerObject(bootstrapProperty);
    }
  }

  private void registerObjects(List<ObjectBootstrapProperty> bootstrapProperties) throws Exception {
    for (ObjectBootstrapProperty bootstrapProperty : bootstrapProperties) {
      registerObject(bootstrapProperty);
    }
  }

  private void registerObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    try {
      if (!bootstrapProperty.getArtifactTypes().contains(artifactType)) {
        return;
      }

      doRegisterObject(bootstrapProperty);
    } catch (InvocationTargetException e) {
      Throwable cause = getCause(e);
      throwException(cause, bootstrapProperty);
    } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException e) {
      throwException(e, bootstrapProperty);
    }
  }

  private void registerTransformers(List<TransformerBootstrapProperty> props) throws Exception {
    for (TransformerBootstrapProperty bootstrapProperty : props) {
      try {
        final Class<? extends Transformer> transformerClass =
            bootstrapProperty.getService().forName(bootstrapProperty.getClassName());

        Class<?> returnClass = null;
        String returnClassString = bootstrapProperty.getReturnClassName();
        if (returnClassString != null) {
          if (returnClassString.equals("byte[]")) {
            returnClass = byte[].class;
          } else {
            returnClass = bootstrapProperty.getService().forName(returnClassString);
          }
        }

        doRegisterTransformer(bootstrapProperty, returnClass, transformerClass);
      } catch (InvocationTargetException e) {
        Throwable cause = getCause(e);
        throwException(cause, bootstrapProperty);
      } catch (NoClassDefFoundError | ClassNotFoundException e) {
        throwException(e, bootstrapProperty);
      }
    }
  }

  protected abstract void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                                Class<? extends Transformer> transformerClass)
      throws Exception;

  protected abstract void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception;

  private void throwException(Throwable t, AbstractBootstrapProperty bootstrapProperty)
      throws Exception {
    if (t instanceof Exception) {
      throw (Exception) t;
    } else {
      throw new Exception(t);
    }
  }

}
