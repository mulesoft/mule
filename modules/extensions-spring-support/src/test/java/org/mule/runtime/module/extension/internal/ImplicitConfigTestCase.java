/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;

import org.junit.Test;

public class ImplicitConfigTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {ImplicitConfigExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "implicit-config.xml";
    }

    @Test
    public void getImplicitConfig() throws Exception
    {
        final Integer defaultValue = 42;
        ImplicitConfigExtension config = (ImplicitConfigExtension) flowRunner("implicitConfig").withPayload("")
                                                                                               .withFlowVariable("optionalWithDefault", defaultValue)
                                                                                               .run()
                                                                                               .getMessage()
                                                                                               .getPayload();


        assertThat(config, is(notNullValue()));
        assertThat(config.getMuleContext(), is(sameInstance(muleContext)));
        assertThat(config.getInitialise(), is(1));
        assertThat(config.getStart(), is(1));
        assertThat(config.getOptionalNoDefault(), is(nullValue()));
        assertThat(config.getOptionalWithDefault(), is(defaultValue));
    }

    @Extension(name = "implicit")
    @Operations({ImplicitOperations.class})
    @Xml(namespaceLocation = "http://www.mulesoft.org/schema/mule/implicit", namespace = "implicit")
    public static class ImplicitConfigExtension implements Initialisable, Startable, MuleContextAware
    {

        private MuleContext muleContext;
        private int initialise = 0;
        private int start = 0;

        @Parameter
        @Optional
        private String optionalNoDefault;

        @Parameter
        @Optional(defaultValue = "#[flowVars['optionalWithDefault']]")
        private Integer optionalWithDefault;

        @Override
        public void initialise() throws InitialisationException
        {
            initialise++;
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            muleContext = context;
        }

        @Override
        public void start() throws MuleException
        {
            start++;
        }

        public MuleContext getMuleContext()
        {
            return muleContext;
        }

        public int getInitialise()
        {
            return initialise;
        }

        public int getStart()
        {
            return start;
        }

        public String getOptionalNoDefault()
        {
            return optionalNoDefault;
        }

        public Integer getOptionalWithDefault()
        {
            return optionalWithDefault;
        }
    }

    public static class ImplicitOperations
    {

        public ImplicitConfigExtension getConfig(@UseConfig ImplicitConfigExtension config)
        {
            return config;
        }
    }
}
