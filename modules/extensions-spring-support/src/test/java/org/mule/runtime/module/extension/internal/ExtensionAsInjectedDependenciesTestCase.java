/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class ExtensionAsInjectedDependenciesTestCase extends ExtensionFunctionalTestCase
{

    private static final String STATIC_HEISENBERG = "staticHeisenberg";
    private static final String DYNAMIC_AGE_HEISENBERG = "dynamicAgeHeisenberg";

    private Dependent dependent;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        dependent = muleContext.getInjector().inject(new Dependent());
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-injected.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }


    @Test
    public void staticHeisenbergWasInjected() throws Exception
    {
        assertCorrectProviderInjected(STATIC_HEISENBERG, dependent.getStaticHeisenberg());
        HeisenbergExtension heisenberg = ExtensionsTestUtils.getConfigurationFromRegistry(STATIC_HEISENBERG, getTestEvent(""));
        assertThat(heisenberg.getPersonalInfo().getAge(), is(50));
    }

    @Test
    public void dynamicHeisenbergWasInjected() throws Exception
    {
        assertCorrectProviderInjected(DYNAMIC_AGE_HEISENBERG, dependent.getDynamicAgeHeisenberg());

        final int age = 52;
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("age", age);

        HeisenbergExtension heisenberg = ExtensionsTestUtils.getConfigurationFromRegistry(DYNAMIC_AGE_HEISENBERG, event);
        assertThat(heisenberg.getPersonalInfo().getAge(), is(age));
    }

    private void assertCorrectProviderInjected(String key, ConfigurationProvider<?> expected)
    {
        assertThat(expected, is(sameInstance(muleContext.getRegistry().get(key))));
    }

    public static class Dependent
    {

        @Inject
        @Named(STATIC_HEISENBERG)
        private ConfigurationProvider<HeisenbergExtension> staticHeisenberg;

        @Inject
        @Named(DYNAMIC_AGE_HEISENBERG)
        private ConfigurationProvider<HeisenbergExtension> dynamicAgeHeisenberg;

        public ConfigurationProvider<HeisenbergExtension> getStaticHeisenberg()
        {
            return staticHeisenberg;
        }

        public ConfigurationProvider<HeisenbergExtension> getDynamicAgeHeisenberg()
        {
            return dynamicAgeHeisenberg;
        }
    }
}
