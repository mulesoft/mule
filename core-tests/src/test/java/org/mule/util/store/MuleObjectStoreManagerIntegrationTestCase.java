/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.api.store.ObjectStoreManager.UNBOUNDED;

import org.mule.api.config.MuleProperties;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
/**
 * Tests MuleObjectStoreManager integration with the wrappers used to monitor
 * each managed object store.
 */
public class MuleObjectStoreManagerIntegrationTestCase extends AbstractMuleContextTestCase
{

    public static final String OBJECT_KEY = "key";
    public static final String OBJECT_KEY_VALUE_1 = "value";
    public static final String OBJECT_KEY_VALUE_2 = "anotherValue";
    private final ObjectStoreFactory objectStoreFactory;

    private enum ObjectStoreType {DEFAULT,USER};

    public MuleObjectStoreManagerIntegrationTestCase(ObjectStoreFactory objectStoreFactory)
    {
        this.objectStoreFactory = objectStoreFactory;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {new ObjectStoreFactory(false, ObjectStoreType.DEFAULT)},
                {new ObjectStoreFactory(true, ObjectStoreType.DEFAULT)},
                {new ObjectStoreFactory(false, ObjectStoreType.USER)},
                {new ObjectStoreFactory(true, ObjectStoreType.USER)}
        });
    }

    @Before
    public void injectMuleContext()
    {
        objectStoreFactory.setMuleObjectStoreManager(muleContext.getRegistry().<MuleObjectStoreManager>get(MuleProperties.OBJECT_STORE_MANAGER));
    }

    @Test
    public void partitionObjectStoreDoesNotCollide() throws Exception
    {
        ObjectStore os = objectStoreFactory.createObjectStore("myOs");
        ObjectStore os2 = objectStoreFactory.createObjectStore("myOs2");
        os.store(OBJECT_KEY, OBJECT_KEY_VALUE_1);
        os2.store(OBJECT_KEY, OBJECT_KEY_VALUE_2);
        assertThat(os.contains(OBJECT_KEY), is(true));
        assertThat((String) os.retrieve(OBJECT_KEY), is(OBJECT_KEY_VALUE_1));
        assertThat(os2.contains(OBJECT_KEY),is(true));
        assertThat((String) os2.retrieve(OBJECT_KEY), is(OBJECT_KEY_VALUE_2));
        assertThat((String) os.remove(OBJECT_KEY), is(OBJECT_KEY_VALUE_1));
        assertThat((String) os2.remove(OBJECT_KEY), is(OBJECT_KEY_VALUE_2));
    }

    @Test
    public void maxEntriesIsHonored() throws Exception
    {
        final int expirationInterval = 1000;
        final int maxEntries = 5;
        final ObjectStore os = objectStoreFactory.createObjectStore("myOs", 5, 0, expirationInterval);

        os.store(0, 0);
        ensureMilisecondChanged();


        for (int i = 1; i < maxEntries +1; i++)
        {
            os.store(i, i);
        }

        PollingProber prober = new PollingProber(expirationInterval * 5, expirationInterval);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test() throws Exception
            {
                assertThat(os.contains(0), is(false));

                for (int i = 1; i < maxEntries + 1; i++)
                {
                    assertThat(os.contains(i), is(true));
                }

                return true;
            }

            @Override
            public String describeFailure()
            {
                return "max entries were not honoured";
            }
        });
    }

    @Test
    public void expirationIntervalWithLowTTL() throws Exception
    {
        int maxEntries = 5;
        int entryTTL = 10;
        int expirationInterval = 100;
        final ListableObjectStore os = objectStoreFactory.createObjectStore("myOs", maxEntries, entryTTL, expirationInterval);

        for (int i = 0; i < maxEntries; i++)
        {
            os.store(i,i);
        }

        PollingProber prober = new PollingProber(1000, expirationInterval);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test() throws Exception
            {
                return os.allKeys().isEmpty();
            }

            @Override
            public String describeFailure()
            {
                return "not all entries were evicted";
            }
        });
    }

    @Test
    public void expirationIntervalWithHighTTLPersistentObjectStore() throws Exception
    {
        int maxEntries = 5;
        int entryTTL = 10000;
        ListableObjectStore os = objectStoreFactory.createObjectStore("myOs", maxEntries, entryTTL,100);

        os.store(0, 0);

        ensureMilisecondChanged();

        for (int i = 1; i < maxEntries; i++)
        {
            os.store(i, i);
        }

        os.store(OBJECT_KEY, OBJECT_KEY_VALUE_1);

        Thread.sleep(entryTTL/5);

        assertThat(os.allKeys().size(), is(maxEntries));

        for (int i = 1; i < maxEntries; i++)
        {
            assertThat(os.contains(i), is(true));
        }

        assertThat(os.contains(OBJECT_KEY), is(true));
    }

    private void ensureMilisecondChanged() throws InterruptedException
    {
        Thread.sleep(2);
    }

    @Test
    public void onlySizeBoundedObjectStore() throws Exception
    {
        final int maxEntries = 5;
        final int entryTTL = UNBOUNDED;

        final ListableObjectStore os = objectStoreFactory.createObjectStore("myOs", maxEntries, entryTTL, 1000);

        os.store(0, 0);

        ensureMilisecondChanged();

        for (int i = 1; i < maxEntries + 1; i++)
        {
            os.store(i, i);
        }

        PollingProber prober = new PollingProber(5000, 1000);
        prober.check(new JUnitProbe()
        {
            @Override
            public boolean test() throws Exception
            {
                assertThat(os.allKeys().size(), is(maxEntries));

                for (int i = 1; i < maxEntries +1; i++)
                {
                    assertThat(os.contains(i), is(true));
                }

                return true;
            }

            @Override
            public String describeFailure()
            {
                return "objectStore was not trimmed";
            }
        });
    }

    @Test
    public void storeUsingBigKey() throws Exception
    {
        ListableObjectStore os = objectStoreFactory.createObjectStore("myOs");
        StringBuilder bigKey = new StringBuilder();
        for (int i = 0; i < 50; i++)
        {
            bigKey.append("abcdefghijklmnopqrstuvwxyz");
        }
        os.store(bigKey.toString(),1);
        assertThat((Integer) os.retrieve(bigKey.toString()), Is.is(1));
    }



    private static class ObjectStoreFactory
    {
        private final boolean isPersistent;
        private final ObjectStoreType objectStoreType;
        private MuleObjectStoreManager muleObjectStoreManager;

        public ObjectStoreFactory(boolean isPersistent, ObjectStoreType objectStoreType)
        {
            this.isPersistent = isPersistent;
            this.objectStoreType = objectStoreType;
        }

        public void setMuleObjectStoreManager(MuleObjectStoreManager muleObjectStoreManager)
        {
            this.muleObjectStoreManager = muleObjectStoreManager;
        }

        public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name)
        {
            if (objectStoreType.equals(ObjectStoreType.USER))
            {
                return muleObjectStoreManager.getUserObjectStore(name,isPersistent);
            }
            else
            {
                return muleObjectStoreManager.getObjectStore(name,isPersistent);
            }
        }

        public <T extends ObjectStore<? extends Serializable>> T createObjectStore(String name, int maxEntries, int entryTTL, int expirationInterval)
        {
            if (objectStoreType.equals(ObjectStoreType.USER))
            {
                return muleObjectStoreManager.getUserObjectStore(name,isPersistent, maxEntries, entryTTL, expirationInterval);
            }
            else
            {
                return muleObjectStoreManager.getObjectStore(name,isPersistent, maxEntries, entryTTL, expirationInterval);
            }
        }
    }

}
