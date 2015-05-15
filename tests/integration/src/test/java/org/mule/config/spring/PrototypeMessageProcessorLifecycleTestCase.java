/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.routing.MessageFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;

public class PrototypeMessageProcessorLifecycleTestCase extends FunctionalTestCase
{

    private static MessageProcessor plainMessageProcessor;

    private static MessageProcessor transformerMessageProcessor;

    private static MessageFilter messageFilter;

    private static AbstractFilteringMessageProcessor filteringMessageProcessor;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/prototype-message-processor-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        plainMessageProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(Initialisable.class));
        transformerMessageProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(Transformer.class, Initialisable.class));
        messageFilter = mock(MessageFilter.class);
        filteringMessageProcessor = mock(AbstractFilteringMessageProcessor.class);
    }

    @Test
    public void noLifecycleOnPlainMessageProcessor() throws Exception
    {
        assertLifecycleOnLookup("plainMessageProcessor", (Initialisable) plainMessageProcessor, false);
    }

    @Test
    public void lifecycleIsAppliedOnTransformer() throws Exception
    {
        assertLifecycleOnLookup("transformer", (Initialisable) transformerMessageProcessor, false);
    }

    @Test
    public void lifecycleIsAppliedOnMessageFilter() throws Exception
    {
        assertLifecycleOnLookup("filter", messageFilter, false);
    }

    @Test
    public void lifecycleIsAppliedOnFilteringMessageProcessor() throws Exception
    {
        assertLifecycleOnLookup("filteringMessageProcessor", messageFilter, false);
    }

    private void assertLifecycleOnLookup(String key, Initialisable expectedObject, boolean applied) throws Exception
    {
        Initialisable messageProcessor = muleContext.getRegistry().lookupObject(key);
        assertThat(messageProcessor, is(sameInstance(expectedObject)));
        verify(messageProcessor, applied ? times(1) : never()).initialise();
    }

    public static class TestPlainMessageProcessorFactoryBean extends BaseTestFactoryBean
    {

        @Override
        public MessageProcessor getObject() throws Exception
        {
            return plainMessageProcessor;
        }

    }

    public static class TestTransformerFactoryBean extends BaseTestFactoryBean
    {

        @Override
        public MessageProcessor getObject() throws Exception
        {
            return transformerMessageProcessor;
        }

    }

    public static class TestMessageFilterFactoryBean extends BaseTestFactoryBean
    {

        @Override
        public MessageProcessor getObject() throws Exception
        {
            return messageFilter;
        }

    }

    public static class TestFilteringMessageProcessorFactoryBean extends BaseTestFactoryBean
    {

        @Override
        public MessageProcessor getObject() throws Exception
        {
            return filteringMessageProcessor;
        }

    }

    private static abstract class BaseTestFactoryBean implements FactoryBean<MessageProcessor>
    {

        @Override
        public Class<?> getObjectType()
        {
            return MessageProcessor.class;
        }

        @Override
        public boolean isSingleton()
        {
            return true;
        }
    }
}
