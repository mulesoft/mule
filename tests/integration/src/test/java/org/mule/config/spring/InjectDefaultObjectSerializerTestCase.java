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
import org.mule.api.config.MuleProperties;
import org.mule.api.serialization.DefaultObjectSerializer;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.serialization.internal.AbstractObjectSerializer;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InjectDefaultObjectSerializerTestCase extends FunctionalTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {"Default Serializer", new String[] {}},
                {"Custom Serializer", new String[] {"custom-object-serializer-config.xml"}}
        });
    }

    private final String name;
    private final String[] configFiles;

    public InjectDefaultObjectSerializerTestCase(String name, String[] configFiles)
    {
        this.name = name;
        this.configFiles = configFiles;
    }

    @Override
    protected String[] getConfigFiles()
    {
        return configFiles;
    }

    @Test
    public void injectObjectSerializer() throws Exception
    {
        TestObjectSerializerInjectionTarget injectionTarget = muleContext.getInjector().inject(new TestObjectSerializerInjectionTarget());
        assertThat(muleContext.getObjectSerializer(), is(sameInstance(injectionTarget.getObjectSerializer())));
        assertThat(injectionTarget.getObjectSerializer(), is(sameInstance(muleContext.getRegistry().get(MuleProperties.OBJECT_SERIALIZER))));
    }

    public static class TestObjectSerializerInjectionTarget
    {

        @Inject
        @DefaultObjectSerializer
        private ObjectSerializer objectSerializer;

        public ObjectSerializer getObjectSerializer()
        {
            return objectSerializer;
        }
    }

    public static class TestObjectSerializer extends AbstractObjectSerializer
    {

        @Override
        protected byte[] doSerialize(Object object) throws Exception
        {
            return new byte[0];
        }

        @Override
        protected <T> T doDeserialize(InputStream inputStream, ClassLoader classLoader) throws Exception
        {
            return null;
        }
    }

}
