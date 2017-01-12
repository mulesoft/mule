/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_METADATA_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.api.metadata.EntityMetadataProvider;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationInstanceNotification;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.NotificationException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * Default implementation of the {@link MetadataService}, which provides access to the Metadata of any Component in the
 * application, using it's {@link ComponentId}. Requires the injection of the {@link MuleContext}, to be able to lookup the
 * component inside the Mule App flows using the given {@link ComponentId}
 *
 * @since 4.0
 */
public class MuleMetadataService implements MetadataService, Initialisable {

  private static final String PROCESSOR_NOT_FOUND = "Processor doesn't exist in the given index [%s]";
  private static final String FLOW_NOT_FOUND = "Flow [%s] doesn't exist";
  private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
  private static final String COMPONENT_NOT_METADATA_PROVIDER =
      "Component [%s] is not a MetadataProvider or MetadataEntityProvider, no information available";
  private static final String COMPONENT_NOT_METADATA_KEY_PROVIDER =
      "Component [%s] is not a MetadataKeyProvider, no information available";
  private static final String EXCEPTION_RESOLVING_COMPONENT_METADATA =
      "An exception occurred while resolving metadata for component '%s'";
  private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Component MetadataKeys";
  private static final String CONFIG_NOT_FOUND = "Configuration named [%s] doesn't exist";

  @Inject
  private MuleContext muleContext;

  private final LoadingCache<String, MetadataCache> caches;

  public MuleMetadataService() {
    caches = CacheBuilder.newBuilder().build(new CacheLoader<String, MetadataCache>() {

      @Override
      public MetadataCache load(String id) throws Exception {
        return new DefaultMetadataCache();
      }
    });
  }

  /**
   * Initialize this instance by registering a {@link CustomNotificationListener}
   *
   * @throws InitialisationException
   */
  @Override
  public void initialise() throws InitialisationException {
    try {
      muleContext.registerListener((CustomNotificationListener<ConfigurationInstanceNotification>) notification -> {
        try {
          if (notification.getAction() == ConfigurationInstanceNotification.CONFIGURATION_STOPPED) {
            String name = ((ConfigurationInstanceNotification) notification).getConfigurationInstance().getName();
            disposeCache(name);
          }
        } catch (Exception e) {
          throw new RuntimeException("Error while looking for the MetadataManager in the registry", e);
        }
      });
    } catch (NotificationException e) {
      throw new InitialisationException(createStaticMessage("Could not register ConfigurationInstanceListener"), e, this);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentId componentId) {
    return exceptionHandledMetadataFetch(() -> findMetadataKeyProvider(componentId).getMetadataKeys(),
                                         EXCEPTION_RESOLVING_METADATA_KEYS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(ComponentId componentId,
                                                                                          MetadataKey key) {
    return getComponentMetadataWithKey(componentId, key, this::lookupProcessor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(ComponentId componentId) {
    return getComponentMetadata(componentId, this::lookupProcessor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(ComponentId componentId,
                                                                                    MetadataKey key) {
    return getComponentMetadataWithKey(componentId, key, this::lookupSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(ComponentId componentId) {
    return getComponentMetadata(componentId, this::lookupSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(ComponentId componentId, MetadataKey key) {
    return exceptionHandledMetadataFetch(() -> findEntityMetadataProvider(componentId).getEntityMetadata(key),
                                         format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys(ComponentId componentId) {
    return exceptionHandledMetadataFetch(() -> findEntityMetadataProvider(componentId).getEntityKeys(),
                                         format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disposeCache(String id) {
    caches.invalidate(id);
  }

  public MetadataCache getMetadataCache(String id) {
    try {
      return caches.get(id);
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get the cache with id:" + id), e);
    }
  }

  public Map<String, ? extends MetadataCache> getMetadataCaches() {
    return copyOf(caches.asMap());
  }

  private <T> MetadataResult<T> exceptionHandledMetadataFetch(MetadataDelegate<T> producer, String failureMessage) {
    try {
      return producer.get();
    } catch (InvalidComponentIdException e) {
      return failure(newFailure(e).withFailureCode(e.getFailureCode()).onComponent());
    } catch (Exception e) {
      return failure(newFailure(e).onComponent());
    }
  }

  private MetadataKeyProvider findMetadataKeyProvider(ComponentId componentId) throws InvalidComponentIdException {
    try {
      return componentId.getFlowName().isPresent() ? (MetadataKeyProvider) lookupComponent(componentId)
          : lookupConfig(componentId.getComponentPath());
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_KEY_PROVIDER, componentId)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private <T extends ComponentModel<T>> MetadataProvider<T> findMetadataProvider(ComponentId componentId,
                                                                                 LookupComponent lookupComponent)
      throws InvalidComponentIdException {
    Flow flow = getFlow(componentId);
    try {
      return (MetadataProvider<T>) lookupComponent.get(flow, componentId);
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_PROVIDER, componentId)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private Flow getFlow(ComponentId componentId) throws InvalidComponentIdException {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(componentId.getFlowName().get());
    if (flow == null) {
      throw new InvalidComponentIdException(createStaticMessage(format(FLOW_NOT_FOUND, componentId.getFlowName().get())),
                                            COMPONENT_NOT_FOUND);
    }
    return flow;
  }

  private EntityMetadataProvider findEntityMetadataProvider(ComponentId componentId) throws InvalidComponentIdException {
    try {
      return (EntityMetadataProvider) lookupComponent(componentId);
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_PROVIDER, componentId)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private Processor lookupProcessor(Flow flow, ComponentId componentId) throws InvalidComponentIdException {
    try {
      return flow.getMessageProcessors().get(Integer.parseInt(componentId.getComponentPath()));
    } catch (IndexOutOfBoundsException | NumberFormatException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(PROCESSOR_NOT_FOUND, componentId.getComponentPath())),
                                            e, COMPONENT_NOT_FOUND);
    }
  }

  private MessageSource lookupSource(Flow flow, ComponentId componentId) throws InvalidComponentIdException {
    final MessageSource messageSource = flow.getMessageSource();
    if (messageSource == null) {
      throw new InvalidComponentIdException(createStaticMessage(SOURCE_NOT_FOUND), COMPONENT_NOT_FOUND);
    }
    return messageSource;
  }

  private MetadataKeyProvider lookupConfig(String configName) throws InvalidComponentIdException {
    MetadataKeyProvider configurationProvider = muleContext.getRegistry().lookupObject(configName);
    if (configurationProvider != null) {
      return configurationProvider;
    } else {
      throw new InvalidComponentIdException(createStaticMessage(format(CONFIG_NOT_FOUND, configName)), COMPONENT_NOT_FOUND);
    }
  }


  private Object lookupComponent(ComponentId componentId) throws InvalidComponentIdException {
    Flow flow = getFlow(componentId);
    return !componentId.getComponentPath().equals("-1") ? lookupProcessor(flow, componentId) : lookupSource(flow, componentId);
  }

  private <T extends ComponentModel<T>> MetadataResult<ComponentMetadataDescriptor<T>> getComponentMetadata(ComponentId componentId,
                                                                                                            LookupComponent lookupComponent) {
    return exceptionHandledMetadataFetch(() -> ((MetadataProvider<T>) findMetadataProvider(componentId, lookupComponent))
        .getMetadata(), format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
  }

  private <T extends ComponentModel<T>> MetadataResult<ComponentMetadataDescriptor<T>> getComponentMetadataWithKey(ComponentId componentId,
                                                                                                                   MetadataKey key,
                                                                                                                   LookupComponent lookupComponent) {
    return exceptionHandledMetadataFetch(() -> ((MetadataProvider<T>) findMetadataProvider(componentId, lookupComponent))
        .getMetadata(key), format(EXCEPTION_RESOLVING_COMPONENT_METADATA, componentId));
  }

  @FunctionalInterface
  private interface MetadataDelegate<T> {

    MetadataResult<T> get() throws MetadataResolvingException, InvalidComponentIdException;
  }

  @FunctionalInterface
  private interface LookupComponent {

    Object get(Flow flow, ComponentId componentId) throws InvalidComponentIdException;
  }
}
