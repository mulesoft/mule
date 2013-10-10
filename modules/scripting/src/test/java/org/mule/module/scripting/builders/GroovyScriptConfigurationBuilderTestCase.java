/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.builders;

import static org.junit.Assert.fail;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractScriptConfigBuilderTestCase;
import org.mule.util.store.QueueStoreAdapter;
import org.mule.util.store.QueuePersistenceObjectStore;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.Properties;

public class GroovyScriptConfigurationBuilderTestCase extends AbstractScriptConfigBuilderTestCase
{
    @Override
    public String getConfigResources()
    {
        return "mule-config.groovy";
    }

    @Override
    public ConfigurationBuilder getBuilder()
    {
        try
        {
            return new ScriptConfigurationBuilder("groovy", getConfigResources());
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
