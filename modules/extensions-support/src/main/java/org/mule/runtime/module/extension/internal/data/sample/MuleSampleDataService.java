/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.data.sample;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver.staticParametersFrom;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.sdk.api.data.sample.SampleDataException.INVALID_LOCATION;
import static org.mule.sdk.api.data.sample.SampleDataException.INVALID_TARGET_EXTENSION;
import static org.mule.sdk.api.data.sample.SampleDataException.NOT_SUPPORTED;
import static org.mule.sdk.api.data.sample.SampleDataException.NO_DATA_AVAILABLE;

import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.data.sample.SampleDataService;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.api.data.sample.ComponentSampleDataProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.tooling.sampledata.SampleDataProviderMediator;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;

/**
 * Default implementation of {@link SampleDataService}
 *
 * @since 4.4.0
 */
public class MuleSampleDataService implements SampleDataService {

  private ConfigurationComponentLocator componentLocator;
  private ExtensionManager extensionManager;
  private MuleContext muleContext;
  private ArtifactEncoding artifactEncoding;
  private StreamingManager streamingManager;
  private ConnectionManager connectionManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public Message getSampleData(Location location) throws SampleDataException {
    Object component = findComponent(location);

    if (component instanceof ComponentSampleDataProvider) {
      Message message = ((ComponentSampleDataProvider) component).getSampleData();
      if (message == null) {
        throw new SampleDataException(format("No Sample Data available for Element at Location [%s]", location),
                                      NO_DATA_AVAILABLE);
      }

      return message;
    }

    throw new SampleDataException(format("Element at Location [%s] is not capable of providing Sample Data", location),
                                  NOT_SUPPORTED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Message getSampleData(String extensionName,
                               String componentName,
                               Map<String, Object> parameters,
                               Supplier<Optional<ConfigurationInstance>> configurationInstanceSupplier)
      throws SampleDataException {

    checkArgument(!isBlank(extensionName), "extensionName cannot be blank");
    checkArgument(!isBlank(componentName), "componentName cannot be blank");
    checkArgument(parameters != null, "parameters cannot be null");

    ExtensionModel extensionModel = extensionManager.getExtension(extensionName)
        .orElseThrow(() -> new SampleDataException(format("Extension '%s' was not found", extensionName),
                                                   INVALID_TARGET_EXTENSION));

    ComponentModel componentModel = extensionModel.findComponentModel(componentName)
        .orElseThrow(() -> new SampleDataException(format("Extension '%s' does not contain any component called '%s''",
                                                          extensionName, componentName),
                                                   INVALID_TARGET_EXTENSION));

    SampleDataProviderMediator mediator = new DefaultSampleDataProviderMediator(
                                                                                extensionModel,
                                                                                componentModel,
                                                                                new ResolvingComponent(extensionName,
                                                                                                       componentName),
                                                                                artifactEncoding,
                                                                                muleContext.getNotificationManager(),
                                                                                new ReflectionCache(),
                                                                                muleContext.getExpressionManager(),
                                                                                streamingManager,
                                                                                muleContext.getInjector(),
                                                                                muleContext);

    ExtensionResolvingContext ctx = new ExtensionResolvingContext(configurationInstanceSupplier, connectionManager);
    try {
      return mediator
          .getSampleData(staticParametersFrom(replaceParameterAliases(parameters, componentModel)),
                         (CheckedSupplier<Object>) () -> ctx.getConnection().orElse(null),
                         (CheckedSupplier<Object>) () -> ctx.getConfig().orElse(null),
                         (CheckedSupplier<ConnectionProvider>) () -> ctx.getConnectionProvider().orElse(null));
    } finally {
      ctx.dispose();
    }
  }

  private Map<String, Object> replaceParameterAliases(Map<String, Object> parameters, ComponentModel model) {
    model.getAllParameterModels().forEach(param -> {
      String paramName = param.getName();
      if (parameters.containsKey(paramName)) {
        parameters.put(getImplementingName(param), parameters.remove(paramName));
      }
    });

    return parameters;
  }

  private Object findComponent(Location location) throws SampleDataException {
    return componentLocator.find(location)
        .orElseThrow(() -> new SampleDataException(format("Invalid location [%s]. No element found at location.", location),
                                                   INVALID_LOCATION));
  }

  @Inject
  public void setComponentLocator(ConfigurationComponentLocator componentLocator) {
    this.componentLocator = componentLocator;
  }

  @Inject
  public void setExtensionManager(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  @Inject
  public void setStreamingManager(StreamingManager streamingManager) {
    this.streamingManager = streamingManager;
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Inject
  public void setArtifactEncoding(ArtifactEncoding artifactEncoding) {
    this.artifactEncoding = artifactEncoding;
  }

  @Inject
  public void setConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  private class ResolvingComponent extends AbstractComponent {

    private final ComponentLocation location;

    public ResolvingComponent(String extensionName, String componentName) {
      location = DefaultComponentLocation.from(extensionName + "/" + componentName);
    }

    @Override
    public ComponentLocation getLocation() {
      return location;
    }

    @Override
    public Location getRootContainerLocation() {
      return Location.builder().globalName(location.getLocation()).build();
    }
  }
}
