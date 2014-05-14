/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class ExtensionsAsInjectedDependenciesTestCase extends ExtensionsFunctionalTestCase
{

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
        ValueResolver<HeisenbergExtension> staticHeisenberg = dependent.getStaticHeisenberg();
        assertThat(staticHeisenberg, is(sameInstance(muleContext.getRegistry().get("staticHeisenberg"))));
        HeisenbergExtension heisenberg = staticHeisenberg.resolve(VoidMuleEvent.getInstance());
        assertThat(heisenberg.getPersonalInfo().getAge(), is(50));
    }

    @Test
    public void dynamicHeisenbergWasInjected() throws Exception
    {
        ValueResolver<HeisenbergExtension> dynamicHeisenberg = dependent.getDynamicAgeHeisenberg();
        final int age = 52;
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("age", age);

        HeisenbergExtension heisenberg = dynamicHeisenberg.resolve(event);
        assertThat(heisenberg.getPersonalInfo().getAge(), is(age));
    }

    public static class Dependent
    {

        @Inject
        @Named("staticHeisenberg")
        private ValueResolver<HeisenbergExtension> staticHeisenberg;

        @Inject
        @Named("dynamicAgeHeisenberg")
        private ValueResolver<HeisenbergExtension> dynamicAgeHeisenberg;

        public ValueResolver<HeisenbergExtension> getStaticHeisenberg()
        {
            return staticHeisenberg;
        }

        public ValueResolver<HeisenbergExtension> getDynamicAgeHeisenberg()
        {
            return dynamicAgeHeisenberg;
        }
    }
}
