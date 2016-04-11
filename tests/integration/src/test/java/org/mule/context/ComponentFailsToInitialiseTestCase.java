/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ComponentFailsToInitialiseTestCase extends FunctionalTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        expectedException.expectCause(CoreMatchers.<InitialisationException>instanceOf(InitialisationException.class));
        FailLifecycleTestObject.setup();
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/component-fail-initialise.xml";
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        assertThat(FailLifecycleTestObject.isInitInvoked(), is(true));
        assertThat(FailLifecycleTestObject.isDisposeInvoked(), is(false));
    }

    @Test
    public void failToInitialise() throws Exception
    {
    }
}
