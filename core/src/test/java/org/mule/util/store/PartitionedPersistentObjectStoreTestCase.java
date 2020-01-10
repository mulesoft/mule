/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;
import static org.mule.util.store.PartitionedPersistentObjectStore.OBJECT_STORE_DIR;

import org.mule.api.MuleContext;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

/**
 *
 */
public class PartitionedPersistentObjectStoreTestCase extends AbstractMuleTestCase
{

    public static final String OBJECT_KEY = "key";
    public static final String OBJECT_BASE_VALUE = "value";
    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private PartitionedPersistentObjectStore<Serializable> os;
    private int numberOfPartitions = 3;

    @Before
    public void setUpMockMuleContext() throws IOException
    {
        numberOfPartitions = 3;
        when(mockMuleContext.getConfiguration().getWorkingDirectory()).thenReturn(".");
        when(mockMuleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        os = new TestPartitionedPersistentObjectStore(mockMuleContext);
        File objectStorePersistDir = new File(PartitionedPersistentObjectStore.OBJECT_STORE_DIR);
        if (objectStorePersistDir.exists())
        {
            FileUtils.deleteDirectory(objectStorePersistDir);
        }

        addJavaSerializerToMockMuleContext(mockMuleContext);
        when(mockMuleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
    }

    @Test
    public void defaultPartitionAndNamedPartitionsDoNotCollide() throws Exception
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void removeEntries() throws Exception
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        removeEntriesInPartitions();
        assertAllPartitionsAreEmpty();
    }

    @Test(expected = ObjectAlreadyExistsException.class)
    public void storeSameKeyThrowsException() throws Exception
    {
        numberOfPartitions = 0;
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void objectStorePersistDataBetweenOpenAndClose() throws ObjectStoreException
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        closePartitions();
        openPartitions();
        assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void clear() throws ObjectStoreException
    {
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
    public void clearDoesntBreakPartition() throws ObjectStoreException
    {
        String partitionName = "custom";
        String key = "key";
        String value = "value";
        os.open(partitionName);
        os.clear(partitionName);
        os.store(key, value, partitionName);
        PartitionedPersistentObjectStore newOS = new PartitionedPersistentObjectStore<>(mockMuleContext);
        newOS.open(partitionName);
        Serializable retrieve = newOS.retrieve(key, partitionName);
        assertThat(retrieve, is((Serializable) value));
    }

    @Test
    public void disposePartitionRemovesPartitionDirectoryByDefault() throws ObjectStoreException
    {
        verifyPartitionFolder(false);
    }

    @Test
    public void disposePartitionRemovesPartitionDirectoryIfPropertySet() throws Exception
    {
        setOnlyClearEntriesOnDispose(true);
        verifyPartitionFolder(true);
        setOnlyClearEntriesOnDispose(false);
    }

    private void verifyPartitionFolder(boolean partionExistsAfterDispose) throws ObjectStoreException
    {
        String partitionName = "custom";
        String key = "key";
        String value = "value";
        File partitionDirectory = getPartitionDirectory(partitionName);
        os.open(partitionName);
        os.store(key, value, partitionName);
        assertThat(partitionDirectory.exists(), equalTo(true));
        os.disposePartition(partitionName);
        assertThat(partitionDirectory.exists(), equalTo(partionExistsAfterDispose));
    }

    private File getPartitionDirectory(String partitionName)
    {
        String workingDirectory = mockMuleContext.getConfiguration().getWorkingDirectory();
        String path = workingDirectory + separator + OBJECT_STORE_DIR + separator + partitionName;
        return new File(path);
    }

    @Test
    public void allowsAnyPartitionName() throws Exception
    {
        os.open("asdfsadfsa#$%@#$@#$@$%$#&8******ASDFWER??!?!");
    }

    @Test
    public void muleContextAwareValueGetsDeserialized() throws Exception
    {
        os.open();
        os.store("key", new DeserializableValue(mockMuleContext));
        DeserializableValue value = (DeserializableValue) os.retrieve("key");
        assertNotNull(value.getMuleContext());
    }

    private void closePartitions() throws ObjectStoreException
    {
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.close(getPartitionName(i));
        }
        os.close();
    }

    private void disposePartitions() throws ObjectStoreException
    {
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.disposePartition(getPartitionName(i));
        }
    }

    private void clearPartitions() throws ObjectStoreException
    {
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.clear(getPartitionName(i));
        }
        os.clear();
    }

    private void assertAllPartitionsAreEmpty() throws ObjectStoreException
    {
        assertThat(os.contains(OBJECT_KEY), is(false));
        assertThat(os.allKeys().size(), is(0));
        for (int i = 0; i < numberOfPartitions; i++)
        {
            assertThat(os.contains(OBJECT_KEY, getPartitionName(i)), is(false));
            assertThat(os.allKeys(getPartitionName(i)).size(), is(0));
        }
    }

    private void removeEntriesInPartitions() throws ObjectStoreException
    {
        os.remove(OBJECT_KEY);
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.remove(OBJECT_KEY, getPartitionName(i));
        }
    }

    private void assertAllValuesExistsInPartitionAreUnique(String key, String value) throws ObjectStoreException
    {
        assertThat((String) os.retrieve(key), is(value));
        for (int i = 0; i < numberOfPartitions; i++)
        {
            assertThat((String) os.retrieve(key, getPartitionName(i)), is(value + i));
        }
    }

    private void assertNotPresentInAnyPartition(String key) throws ObjectStoreException
    {
        Assert.assertFalse(os.contains(key));
        for (int i = 0; i < numberOfPartitions; i++)
        {
            Assert.assertFalse(os.contains(key, getPartitionName(i)));
        }
    }

    private void storeInPartitions(String key, String value) throws ObjectStoreException
    {
        os.store(key, value);
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.store(key, value + i, getPartitionName(i));
        }
    }

    private void openPartitions() throws ObjectStoreException
    {
        os.open();
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.open(getPartitionName(i));
        }
    }

    private String getPartitionName(int i)
    {
        return "partition" + i;
    }

    public static class DeserializableValue implements DeserializationPostInitialisable, Serializable
    {
        private transient MuleContext muleContext;

        public DeserializableValue(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        public void initAfterDeserialisation(MuleContext muleContext)
        {
            this.muleContext = muleContext;
        }

        public MuleContext getMuleContext()
        {
            return muleContext;
        }
    }

    private static class TestPartitionedPersistentObjectStore extends PartitionedPersistentObjectStore<Serializable>
    {
        public TestPartitionedPersistentObjectStore(MuleContext mockMuleContext)
        {
            super(mockMuleContext);
        }

        @Override
        protected String getPartitionDirectoryName(String partitionName)
        {
            return partitionName;
        }
    }

    private void setOnlyClearEntriesOnDispose(boolean value) throws Exception
    {
        Field onlyClearEntriesOnDisposeField = PartitionedPersistentObjectStore.class.getDeclaredField("onlyClearEntriesOnDispose");
        onlyClearEntriesOnDisposeField.setAccessible(TRUE);
        onlyClearEntriesOnDisposeField.set(null, value);
    }

}
