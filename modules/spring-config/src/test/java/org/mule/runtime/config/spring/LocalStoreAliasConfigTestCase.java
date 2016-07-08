/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertSame;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class LocalStoreAliasConfigTestCase extends AbstractMuleContextTestCase
{

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(new String[0], emptyMap(), APP);
    }

    @Test
    public void inMemoryObjectStore() throws Exception
    {
        this.testSame(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, DefaultMuleContext.LOCAL_TRANSIENT_OBJECT_STORE_KEY);
    }

    @Test
    public void persistentObjectStore() throws Exception
    {
        this.testSame(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME, DefaultMuleContext.LOCAL_PERSISTENT_OBJECT_STORE_KEY);
    }


    @Test
    public void userObjectStore() throws Exception
    {
        this.testSame(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME, "_localUserObjectStore");
    }

    @Test
    public void transientUserObjectStore() throws Exception
    {
        this.testSame(MuleProperties.DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME, "_localTransientUserObjectStore");
    }

    @Test
    public void queueManager() throws Exception
    {
        this.testSame(MuleProperties.OBJECT_QUEUE_MANAGER, DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY);
        assertSame(muleContext.getQueueManager(), muleContext.getRegistry().lookupObject(DefaultMuleContext.LOCAL_QUEUE_MANAGER_KEY));
    }

    @Test
    public void objectStoreManager() throws Exception
    {
        this.testSame(MuleProperties.OBJECT_STORE_MANAGER, DefaultMuleContext.LOCAL_OBJECT_STORE_MANAGER_KEY);
        assertSame(muleContext.getObjectStoreManager(), muleContext.getRegistry().lookupObject(DefaultMuleContext.LOCAL_OBJECT_STORE_MANAGER_KEY));
    }

    private void testSame(String key1, String key2)
    {
        Object obj1 = muleContext.getRegistry().lookupObject(key1);
        Object obj2 = muleContext.getRegistry().lookupObject(key2);
        assertSame(obj1, obj2);
    }
}
