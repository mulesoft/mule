/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class PartitionedInMemoryObjectStoreTestCase extends AbstractMuleTestCase
{

    private static final String TEST_PARTITION = "testPartition";
    private static final String TEST_VALUE = "testValue";
    private static final String TEST_KEY1 = "testKey1";
    private static final String TEST_KEY2 = "testKey2";
    private static final String TEST_KEY3 = "testKey3";
    private static final int GC_TIMEOUT_MILLIS = 10000;

    private PartitionedInMemoryObjectStore<String> store;

    private long currentNanoTime = MILLISECONDS.toNanos(1);

    @Before
    public void setup()
    {
        store = new PartitionedInMemoryObjectStore()
        {
            @Override
            protected long getCurrentNanoTime()
            {
                return currentNanoTime;
            }
        };
    }

    @Test
    public void expireByTtlMultipleKeysInsertedInTheSameNanoSecond() throws ObjectStoreException
    {
        store.store(TEST_KEY1, TEST_VALUE, TEST_PARTITION);
        store.store(TEST_KEY2, TEST_VALUE, TEST_PARTITION);

        currentNanoTime = MILLISECONDS.toNanos(2);

        store.store(TEST_KEY3, TEST_VALUE, TEST_PARTITION);
        store.expire(1, 100, TEST_PARTITION);

        assertThat(store.contains(TEST_KEY1, TEST_PARTITION), is(false));
        assertThat(store.contains(TEST_KEY2, TEST_PARTITION), is(false));
        assertThat(store.retrieve(TEST_KEY3, TEST_PARTITION), equalTo(TEST_VALUE));
    }

    @Test
    public void expireByNumberOfEntriesMultipleKeysInsertedInTheSameNanoSecond() throws ObjectStoreException
    {
        store.store(TEST_KEY1, TEST_VALUE, TEST_PARTITION);
        store.store(TEST_KEY2, TEST_VALUE, TEST_PARTITION);

        currentNanoTime = MILLISECONDS.toNanos(2);

        store.store(TEST_KEY3, TEST_VALUE, TEST_PARTITION);
        store.expire(10, 1, TEST_PARTITION);

        assertThat(store.contains(TEST_KEY1, TEST_PARTITION), is(false));
        assertThat(store.contains(TEST_KEY2, TEST_PARTITION), is(false));
        assertThat(store.retrieve(TEST_KEY3, TEST_PARTITION), equalTo(TEST_VALUE));
    }

    @Test
    public void removeKeyInsertedInTheSameNanosecondThanOther() throws ObjectStoreException
    {
        store.store(TEST_KEY1, TEST_VALUE, TEST_PARTITION);
        store.store(TEST_KEY2, TEST_VALUE, TEST_PARTITION);

        currentNanoTime = MILLISECONDS.toNanos(2);

        store.store(TEST_KEY3, TEST_VALUE, TEST_PARTITION);

        store.remove(TEST_KEY2, TEST_PARTITION);

        assertThat(store.retrieve(TEST_KEY1, TEST_PARTITION), equalTo(TEST_VALUE));
        assertThat(store.contains(TEST_KEY2, TEST_PARTITION), is(false));
        assertThat(store.retrieve(TEST_KEY3, TEST_PARTITION), equalTo(TEST_VALUE));
    }

    @Test
    public void removesDataOnClear() throws ObjectStoreException
    {
        TrackedKey trackedKey = new TrackedKey();
        final PhantomReference<TrackedKey> keyRef = trackAndStore(trackedKey);

        store.clear(TEST_PARTITION);
        assertThat(store.contains(trackedKey, TEST_PARTITION), is(false));
        trackedKey = null;

        validateClear(keyRef);
    }

    @Test
    public void removesDataOnClose() throws ObjectStoreException
    {
        TrackedKey trackedKey = new TrackedKey();
        final PhantomReference<TrackedKey> keyRef = trackAndStore(trackedKey);

        store.close(TEST_PARTITION);
        assertThat(store.contains(trackedKey, TEST_PARTITION), is(false));
        trackedKey = null;

        validateClear(keyRef);
    }

    private PhantomReference<TrackedKey> trackAndStore(TrackedKey trackedKey) throws ObjectStoreException
    {
        final PhantomReference<TrackedKey> keyRef = new PhantomReference<>(trackedKey, new ReferenceQueue<>());

        store.store(trackedKey, TEST_VALUE, TEST_PARTITION);
        assertThat(store.contains(trackedKey, TEST_PARTITION), is(true));
        return keyRef;
    }

    private void validateClear(final PhantomReference<TrackedKey> keyRef) throws ObjectStoreException
    {
        new PollingProber(GC_TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL).check(new Probe() {

            @Override
            public boolean isSatisfied()
            {
                System.gc();
                return keyRef.isEnqueued();
            }

            @Override
            public String describeFailure()
            {
                return "A hard reference is being maintained to the entry expiration info.";
            }
        });
    }

    private class TrackedKey implements Serializable
    {
    }




}
