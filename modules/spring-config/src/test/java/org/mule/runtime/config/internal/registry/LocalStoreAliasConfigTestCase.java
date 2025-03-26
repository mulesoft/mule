/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_IN_MEMORY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCAL_STORE_PERSISTENT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_QUEUE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.LOCAL_OBJECT_STORE_MANAGER_KEY;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY;

import static java.util.Collections.emptyMap;

import static org.junit.Assert.assertSame;

import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class LocalStoreAliasConfigTestCase extends AbstractMuleContextTestCase {

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new ArtifactAstConfigurationBuilder(emptyArtifact(), emptyMap());
  }

  @Test
  public void inMemoryObjectStore() throws Exception {
    this.testSame(BASE_IN_MEMORY_OBJECT_STORE_KEY, OBJECT_LOCAL_STORE_IN_MEMORY);
  }

  @Test
  public void persistentObjectStore() throws Exception {
    this.testSame(BASE_PERSISTENT_OBJECT_STORE_KEY, OBJECT_LOCAL_STORE_PERSISTENT);
  }

  @Test
  public void queueManager() throws Exception {
    this.testSame(OBJECT_QUEUE_MANAGER, LOCAL_QUEUE_MANAGER_KEY);
    assertSame(muleContext.getQueueManager(),
               ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(LOCAL_QUEUE_MANAGER_KEY));
  }

  @Test
  public void objectStoreManager() throws Exception {
    this.testSame(OBJECT_STORE_MANAGER, LOCAL_OBJECT_STORE_MANAGER_KEY);
    assertSame(muleContext.getObjectStoreManager(),
               ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(LOCAL_OBJECT_STORE_MANAGER_KEY));
  }

  private void testSame(String key1, String key2) {
    Object obj1 = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(key1);
    Object obj2 = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(key2);
    assertSame(obj1, obj2);
  }
}
