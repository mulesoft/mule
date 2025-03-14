/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_METADATA_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.metadata.internal.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;

import static java.lang.String.format;

import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
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
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.RouterInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ScopeInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metadata.internal.cache.MetadataCacheManager;

import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Default implementation of the {@link MetadataService}, which provides access to the Metadata of any Component in the
 * application, using it's {@link Location}. Requires the injection of the {@link MuleContext}, to be able to lookup the component
 * inside the Mule App flows using the given {@link Location}
 *
 * @since 4.0
 */
public class MuleMetadataService implements MetadataService {

  private static final String COMPONENT_NOT_METADATA_PROVIDER =
      "Component [%s] is not a MetadataProvider or MetadataEntityProvider, no information available";
  private static final String COMPONENT_NOT_METADATA_KEY_PROVIDER =
      "Component [%s] is not a MetadataKeyProvider, no information available";
  private static final String EXCEPTION_RESOLVING_COMPONENT_METADATA =
      "An exception occurred while resolving metadata for component '%s'";
  private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Component MetadataKeys";

  @Inject
  private ConfigurationComponentLocator componentLocator;

  @Inject
  @Named(METADATA_CACHE_MANAGER_KEY)
  private MetadataCacheManager cacheManager;

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
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(Location location, MetadataKey partialKey) {
    return exceptionHandledMetadataFetch(() -> findMetadataKeyProvider(location).getMetadataKeys(partialKey),
                                         EXCEPTION_RESOLVING_METADATA_KEYS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<InputMetadataDescriptor> getInputMetadata(Location location, MetadataKey key) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getInputMetadata(key),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
  }

  @Override
  public MetadataResult<ScopeInputMetadataDescriptor> getScopeInputMetadata(Location location,
                                                                            MetadataKey key,
                                                                            Supplier<MessageMetadataType> scopeInputMessageType) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getScopeInputMetadata(key, scopeInputMessageType),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
  }

  @Override
  public MetadataResult<RouterInputMetadataDescriptor> getRouterInputMetadata(Location location,
                                                                              MetadataKey key,
                                                                              Supplier<MessageMetadataType> routerInputMessageType) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getRouterInputMetadata(key, routerInputMessageType),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<OutputMetadataDescriptor> getOutputMetadata(Location location, MetadataKey key) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getOutputMetadata(key),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
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

  @Override
  public MetadataResult<OutputMetadataDescriptor> getScopeOutputMetadata(Location location,
                                                                         MetadataKey key,
                                                                         ScopeOutputMetadataContext ctx) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getScopeOutputMetadata(key, ctx),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getRouterOutputMetadata(Location location, MetadataKey key,
                                                                          RouterOutputMetadataContext ctx) {
    return exceptionHandledMetadataFetch(() -> findMetadataProvider(location).getRouterOutputMetadata(key, ctx),
                                         EXCEPTION_RESOLVING_COMPONENT_METADATA);
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
    cacheManager.dispose(id);
  }

  public MetadataCache getMetadataCache(String id) {
    try {
      return cacheManager.getOrCreateCache(id);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("An error occurred while obtaCould not get the cache with id '" + id
          + "':" + e.getMessage()),
                                     e);
    }
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

  public void saveCache(String id, MetadataCache cache) {
    cacheManager.updateCache(id, cache);
  }

  @FunctionalInterface
  private interface MetadataDelegate<T> {

    MetadataResult<T> get() throws MetadataResolvingException, InvalidComponentIdException;
  }

}
