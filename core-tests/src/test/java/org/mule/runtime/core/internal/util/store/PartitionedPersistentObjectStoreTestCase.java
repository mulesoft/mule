/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

/**
 *
 */
public class PartitionedPersistentObjectStoreTestCase extends AbstractMuleTestCase {

  private static final String OBJECT_KEY = "key";
  private static final String OBJECT_BASE_VALUE = "value";

  private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
  private PartitionedPersistentObjectStore<Serializable> os;
  private int numberOfPartitions = 3;

  @Before
  public void setUpMockMuleContext() throws IOException {
    numberOfPartitions = 3;
    when(mockMuleContext.getConfiguration().getWorkingDirectory()).thenReturn(".");
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
    os = new PartitionedPersistentObjectStore<>(mockMuleContext);
    File objectStorePersistDir = new File(PartitionedPersistentObjectStore.OBJECT_STORE_DIR);
    if (objectStorePersistDir.exists()) {
      FileUtils.deleteDirectory(objectStorePersistDir);
    }

    addJavaSerializerToMockMuleContext(mockMuleContext);
    when(mockMuleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
  }

  @Test
  public void defaultPartitionAndNamedPartitionsDoNotCollide() throws Exception {
    openPartitions();
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
  }

  @Test
  public void removeEntries() throws Exception {
    openPartitions();
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    removeEntriesInPartitions();
    assertAllPartitionsAreEmpty();
  }

  @Test(expected = ObjectAlreadyExistsException.class)
  public void storeSameKeyThrowsException() throws Exception {
    numberOfPartitions = 0;
    openPartitions();
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
  }

  @Test
  public void objectStorePersistDataBetweenOpenAndClose() throws ObjectStoreException {
    openPartitions();
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    closePartitions();
    openPartitions();
    assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
  }

  @Test
  public void clear() throws ObjectStoreException {
    this.openPartitions();
    storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);

    this.clearPartitions();
    this.assertNotPresentInAnyPartition(OBJECT_KEY);

    // assert is reusable
    this.storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    this.assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
  }

  @Test
  public void allowsAnyPartitionName() throws Exception {
    os.open("asdfsadfsa#$%@#$@#$@$%$#&8******ASDFWER??!?!");
  }

  @Test
  public void muleContextAwareValueGetsDeserialized() throws Exception {
    os.open();
    os.store("key", new DeserializableValue(mockMuleContext));
    DeserializableValue value = (DeserializableValue) os.retrieve("key");
    assertNotNull(value.getMuleContext());
  }

  private void closePartitions() throws ObjectStoreException {
    for (int i = 0; i < numberOfPartitions; i++) {
      os.close(getPartitionName(i));
    }
    os.close();
  }

  private void clearPartitions() throws ObjectStoreException {
    for (int i = 0; i < numberOfPartitions; i++) {
      os.clear(getPartitionName(i));
    }
    os.clear();
  }

  private void assertAllPartitionsAreEmpty() throws ObjectStoreException {
    assertThat(os.contains(OBJECT_KEY), is(false));
    assertThat(os.allKeys().size(), is(0));
    for (int i = 0; i < numberOfPartitions; i++) {
      assertThat(os.contains(OBJECT_KEY, getPartitionName(i)), is(false));
      assertThat(os.allKeys(getPartitionName(i)).size(), is(0));
    }
  }

  private void removeEntriesInPartitions() throws ObjectStoreException {
    os.remove(OBJECT_KEY);
    for (int i = 0; i < numberOfPartitions; i++) {
      os.remove(OBJECT_KEY, getPartitionName(i));
    }
  }

  private void assertAllValuesExistsInPartitionAreUnique(String key, String value) throws ObjectStoreException {
    assertThat(os.retrieve(key), is(value));
    for (int i = 0; i < numberOfPartitions; i++) {
      assertThat(os.retrieve(key, getPartitionName(i)), is(value + i));
    }
  }

  private void assertNotPresentInAnyPartition(String key) throws ObjectStoreException {
    Assert.assertFalse(os.contains(key));
    for (int i = 0; i < numberOfPartitions; i++) {
      Assert.assertFalse(os.contains(key, getPartitionName(i)));
    }
  }

  private void storeInPartitions(String key, String value) throws ObjectStoreException {
    os.store(key, value);
    for (int i = 0; i < numberOfPartitions; i++) {
      os.store(key, value + i, getPartitionName(i));
    }
  }

  private void openPartitions() throws ObjectStoreException {
    os.open();
    for (int i = 0; i < numberOfPartitions; i++) {
      os.open(getPartitionName(i));
    }
  }

  private String getPartitionName(int i) {
    return "partition" + i;
  }

  public static class DeserializableValue implements DeserializationPostInitialisable, Serializable {

    private transient MuleContext muleContext;

    public DeserializableValue(MuleContext muleContext) {
      this.muleContext = muleContext;
    }

    public void initAfterDeserialisation(MuleContext muleContext) {
      this.muleContext = muleContext;
    }

    public MuleContext getMuleContext() {
      return muleContext;
    }
  }

}
