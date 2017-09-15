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
import static org.mule.runtime.core.internal.config.ConfigurationInstanceNotification.CONFIGURATION_STOPPED;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
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
import org.mule.runtime.core.internal.config.ConfigurationInstanceNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * Default implementation of the {@link MetadataService}, which provides access to the Metadata of any Component in the
 * application, using it's {@link Location}. Requires the injection of the {@link MuleContext}, to be able to lookup the component
 * inside the Mule App flows using the given {@link Location}
 *
 * @since 4.0
 */
public class MuleMetadataService implements MetadataService, Initialisable {

  private static final String COMPONENT_NOT_METADATA_PROVIDER =
      "Component [%s] is not a MetadataProvider or MetadataEntityProvider, no information available";
  private static final String COMPONENT_NOT_METADATA_KEY_PROVIDER =
      "Component [%s] is not a MetadataKeyProvider, no information available";
  private static final String EXCEPTION_RESOLVING_COMPONENT_METADATA =
      "An exception occurred while resolving metadata for component '%s'";
  private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Component MetadataKeys";

  @Inject
  private NotificationListenerRegistry notificationRegistrer;

  @Inject
  private ConfigurationComponentLocator componentLocator;

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
    notificationRegistrer.registerListener((CustomNotificationListener<ConfigurationInstanceNotification>) notification -> {
      try {
        if (notification.getAction().getActionId() == CONFIGURATION_STOPPED) {
          String name = ((ConfigurationInstanceNotification) notification).getConfigurationInstance().getName();
          disposeCache(name);
        }
      } catch (Exception e) {
        throw new RuntimeException("Error while looking for the MetadataManager in the registry", e);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(Location location) {
    return exceptionHandledMetadataFetch(() -> findMetadataKeyProvider(location).getMetadataKeys(),
                                         EXCEPTION_RESOLVING_METADATA_KEYS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location,
                                                                                          MetadataKey key) {
    return getComponentMetadataWithKey(location, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location) {
    return getComponentMetadata(location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location,
                                                                                    MetadataKey key) {
    return getComponentMetadataWithKey(location, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location) {
    return getComponentMetadata(location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(Location location, MetadataKey key) {
    return exceptionHandledMetadataFetch(() -> findEntityMetadataProvider(location).getEntityMetadata(key),
                                         format(EXCEPTION_RESOLVING_COMPONENT_METADATA, location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys(Location location) {
    return exceptionHandledMetadataFetch(() -> findEntityMetadataProvider(location).getEntityKeys(),
                                         format(EXCEPTION_RESOLVING_COMPONENT_METADATA, location));
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

  private MetadataKeyProvider findMetadataKeyProvider(Location location) throws InvalidComponentIdException {
    try {
      return (MetadataKeyProvider) findComponent(location);
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_KEY_PROVIDER, location)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private Object findComponent(Location location) throws InvalidComponentIdException {
    return componentLocator.find(location)
        .orElseThrow(() -> new InvalidComponentIdException(createStaticMessage("No object found with location " + location),
                                                           COMPONENT_NOT_FOUND));
  }

  private <T extends ComponentModel> MetadataProvider<T> findMetadataProvider(Location location)
      throws InvalidComponentIdException {
    try {
      return (MetadataProvider<T>) findComponent(location);
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_PROVIDER, location)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private EntityMetadataProvider findEntityMetadataProvider(Location location) throws InvalidComponentIdException {
    try {
      return (EntityMetadataProvider) findComponent(location);
    } catch (ClassCastException e) {
      throw new InvalidComponentIdException(createStaticMessage(format(COMPONENT_NOT_METADATA_PROVIDER, location)),
                                            NO_DYNAMIC_METADATA_AVAILABLE);
    }
  }

  private <T extends ComponentModel> MetadataResult<ComponentMetadataDescriptor<T>> getComponentMetadata(Location location) {
    return exceptionHandledMetadataFetch(() -> ((MetadataProvider<T>) findMetadataProvider(location))
        .getMetadata(), format(EXCEPTION_RESOLVING_COMPONENT_METADATA, location));
  }

  private <T extends ComponentModel> MetadataResult<ComponentMetadataDescriptor<T>> getComponentMetadataWithKey(Location location,
                                                                                                                MetadataKey key) {
    return exceptionHandledMetadataFetch(() -> ((MetadataProvider<T>) findMetadataProvider(location))
        .getMetadata(key), format(EXCEPTION_RESOLVING_COMPONENT_METADATA, location));
  }

  @FunctionalInterface
  private interface MetadataDelegate<T> {

    MetadataResult<T> get() throws MetadataResolvingException, InvalidComponentIdException;
  }

}
