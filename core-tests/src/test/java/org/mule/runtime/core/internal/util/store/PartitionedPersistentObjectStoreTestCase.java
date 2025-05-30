/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.store;

import static org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore.OBJECT_STORE_DIR;

import static java.io.File.separator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.runtime.core.internal.store.PartitionedPersistentObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class PartitionedPersistentObjectStoreTestCase extends AbstractMuleTestCase {

  private static final String OBJECT_KEY = "key";
  private static final String OBJECT_BASE_VALUE = "value";

  private MuleConfiguration muleConfiguration;
  private JavaObjectSerializer javaObjectSerializer;
  private PartitionedPersistentObjectStore<Serializable> os;
  private int numberOfPartitions = 3;

  @Before
  public void setUpMockMuleContext() throws IOException {
    numberOfPartitions = 3;
    muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getWorkingDirectory()).thenReturn(".");
    javaObjectSerializer = new JavaObjectSerializer(this.getClass().getClassLoader());
    os = new TestPartitionedPersistentObjectStore(muleConfiguration, javaObjectSerializer);
    File objectStorePersistDir = new File(PartitionedPersistentObjectStore.OBJECT_STORE_DIR);
    if (objectStorePersistDir.exists()) {
      FileUtils.deleteDirectory(objectStorePersistDir);
    }
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
  public void clearDoesntBreakPartition() throws ObjectStoreException {
    String partitionName = "custom";
    String key = "key";
    String value = "value";

    os.open(partitionName);
    os.clear(partitionName);
    os.store(key, value, partitionName);

    PartitionedPersistentObjectStore newOS =
        new PartitionedPersistentObjectStore<>(muleConfiguration, javaObjectSerializer);

    newOS.open(partitionName);
    Serializable retrieve = newOS.retrieve(key, partitionName);
    assertThat(retrieve, is(value));
  }

  @Test
  public void disposePartitionRemovesPartitionDirectory() throws ObjectStoreException {
    String partitionName = "custom";
    String key = "key";
    String value = "value";
    File partitionDirectory = getPartitionDirectory(partitionName);
    os.open(partitionName);
    os.store(key, value, partitionName);
    assertThat(partitionDirectory.exists(), equalTo(true));
    os.disposePartition(partitionName);
    assertThat(partitionDirectory.exists(), equalTo(false));
  }

  private File getPartitionDirectory(String partitionName) {
    String workingDirectory = muleConfiguration.getWorkingDirectory();
    String path = workingDirectory + separator + OBJECT_STORE_DIR + separator + partitionName;
    return new File(path);
  }

  @Test
  public void allowsAnyPartitionName() throws Exception {
    os.open("asdfsadfsa#$%@#$@#$@$%$#&8ASDFWER!!");
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

  private static class TestPartitionedPersistentObjectStore extends PartitionedPersistentObjectStore<Serializable> {

    public TestPartitionedPersistentObjectStore(MuleConfiguration muleConfiguration, ObjectSerializer serializer) {
      super(muleConfiguration, serializer);
    }

    @Override
    protected String getPartitionDirectoryName(String partitionName) {
      return partitionName;
    }
  }

}
