/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class LookupPrototypeByKeyTestCase extends FunctionalTestCase
{

    private static final String PROTOTYPE_KEY = "prototype";

    @Override
    protected String getConfigFile()
    {
        return "prototype-lifecycle-object-config.xml";
    }

    @Test
    public void lookupWithLifecycle() throws Exception
    {
        TestLifecycleObject object = muleContext.getRegistry().lookupObject(PROTOTYPE_KEY, true);
        assertLifecycleApplied(object);
    }

    @Test
    public void lookupWithoutLifecycle() throws Exception
    {
        TestLifecycleObject object = muleContext.getRegistry().lookupObject(PROTOTYPE_KEY, false);
        assertLifecycleNotApplied(object);
    }

    @Test
    public void defaultLookupPrototype() throws Exception
    {
        TestLifecycleObject object = muleContext.getRegistry().lookupObject(PROTOTYPE_KEY);
        assertLifecycleApplied(object);
    }

    private void assertLifecycleApplied(TestLifecycleObject object)
    {
        assertThat(object.getInitialise(), is(1));
        assertThat(object.getStart(), is(1));
    }

    private void assertLifecycleNotApplied(TestLifecycleObject object)
    {
        assertThat(object.getInitialise(), is(0));
        assertThat(object.getStart(), is(0));
    }
}
