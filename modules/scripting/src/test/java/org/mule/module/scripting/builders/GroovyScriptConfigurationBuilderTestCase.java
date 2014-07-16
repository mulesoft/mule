/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.builders;

import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;
import org.mule.util.store.QueuePersistenceObjectStore;
import org.mule.util.store.QueueStoreAdapter;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Properties;

public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{
    @Override
    public String getConfigFile()
    {
        return "mule-config.groovy";
    }

    @Override
    public ConfigurationBuilder getBuilder()
    {
        try
        {
            return new ScriptConfigurationBuilder("groovy", getConfigFile());
        }
        catch (MuleException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties superProps = super.getStartUpProperties();
        Properties props = superProps == null ? new Properties() : new Properties(superProps);
        props.put(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME, new QueueStoreAdapter<Serializable>(new SimpleMemoryObjectStore<Serializable>()));
        props.put(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME, new QueueStoreAdapter<Serializable>(new QueuePersistenceObjectStore<Serializable>()));
        props.put(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME, new SimpleMemoryObjectStore<Serializable>());
        props.put(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME, new QueuePersistenceObjectStore<Serializable>());

        return props;
    }
}
