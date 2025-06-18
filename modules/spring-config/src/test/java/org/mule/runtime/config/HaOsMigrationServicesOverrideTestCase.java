/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static java.lang.System.setProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.core.internal.config.DefaultCustomizationService.HA_MIGRATION_ENABLED_PROPERTY;
import org.junit.jupiter.api.Test;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HaOsMigrationServicesOverrideTestCase {

  private DefaultCustomizationService customizationService = new DefaultCustomizationService();

  @Test
  void doNotCombineServicesIfNotInMigration() throws Exception {
    setProperty(HA_MIGRATION_ENABLED_PROPERTY, "false");
    TestObjectStore os1 = new TestObjectStore();
    TestObjectStore os2 = new TestObjectStore();
    customizationService.interceptDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                                     interceptor -> interceptor.overrideServiceImpl(os1));
    customizationService.interceptDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                                     interceptor -> interceptor.overrideServiceImpl(os2));
    ObjectStore finalStore =
        (ObjectStore) customizationService.getOverriddenService(BASE_IN_MEMORY_OBJECT_STORE_KEY).get().getServiceImpl().get();
    finalStore.store("cow", "moo");
    assertThat(os1.contains("cow"), is(false));
    assertThat(os2.contains("cow"), is(true));
  }

  @Test
  void combineServicesIfInMigration() throws Exception {
    setProperty(HA_MIGRATION_ENABLED_PROPERTY, "true");
    TestObjectStore os1 = new TestObjectStore();
    TestObjectStore os2 = new TestObjectStore();
    customizationService.interceptDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                                     interceptor -> interceptor.overrideServiceImpl(os1));
    customizationService.interceptDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY,
                                                     interceptor -> interceptor.overrideServiceImpl(os2));
    ObjectStore finalStore =
        (ObjectStore) customizationService.getOverriddenService(BASE_IN_MEMORY_OBJECT_STORE_KEY).get().getServiceImpl().get();
    finalStore.store("cow", "moo");
    try {
      assertThat(os1.contains("cow"), is(true));
      assertThat(os2.contains("cow"), is(true));
    } finally {
      setProperty(HA_MIGRATION_ENABLED_PROPERTY, "false");
    }
  }


  private static final class TestObjectStore implements ObjectStore<String> {

    private final Map<String, String> memory = new HashMap<>();

    @Override
    public boolean contains(String key) throws ObjectStoreException {
      return memory.containsKey(key);
    }

    @Override
    public void store(String key, String value) throws ObjectStoreException {
      memory.put(key, value);
    }

    @Override
    public String retrieve(String key) throws ObjectStoreException {
      return memory.get(key);
    }

    @Override
    public String remove(String key) throws ObjectStoreException {
      return memory.remove(key);
    }

    @Override
    public boolean isPersistent() {
      return false;
    }

    @Override
    public void clear() throws ObjectStoreException {
      memory.clear();
    }

    @Override
    public void open() throws ObjectStoreException {

    }

    @Override
    public void close() throws ObjectStoreException {

    }

    @Override
    public List<String> allKeys() throws ObjectStoreException {
      return memory.keySet().stream().toList();
    }

    @Override
    public Map<String, String> retrieveAll() throws ObjectStoreException {
      return memory;
    }
  }

}
