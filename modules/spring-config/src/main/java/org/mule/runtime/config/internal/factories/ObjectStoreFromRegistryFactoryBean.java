/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.store.ObjectStore;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.io.Serializable;

import javax.inject.Inject;

/**
 * Lookup an {@link ObjectStore} from the registry.
 */
public class ObjectStoreFromRegistryFactoryBean extends AbstractFactoryBean<ObjectStore<Serializable>> {

  private String objectStoreName;

  @Inject
  private Registry registry;

  public ObjectStoreFromRegistryFactoryBean(String name) {
    super();
    objectStoreName = name;
  }

  @Override
  public Class<?> getObjectType() {
    return ObjectStore.class;
  }

  @Override
  protected ObjectStore<Serializable> createInstance() throws Exception {
    return registry.<ObjectStore>lookupByName(objectStoreName).get();
  }
}
