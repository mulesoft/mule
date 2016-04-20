/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.Ricin;

import org.junit.Test;

public class SingleConfigParserTestCase extends AbstractConfigParserTestCase
{

    @Test
    public void configWithExpressionFunctionIsSameInstanceForDifferentEvents() throws Exception
    {
        MuleEvent event = getHeisenbergEvent();
        MuleEvent anotherEvent = getTestEvent("");
        HeisenbergExtension config = lookupHeisenberg(HEISENBERG_BYNAME, event);
        HeisenbergExtension anotherConfig = lookupHeisenberg(HEISENBERG_BYNAME, anotherEvent);
        assertThat(config, is(sameInstance(anotherConfig)));
    }

    @Test
    public void configWithExpressionFunctionStillDynamic() throws Exception
    {
        MuleEvent event = getHeisenbergEvent();
        MuleEvent anotherEvent = getHeisenbergEvent();
        anotherEvent.setFlowVariable("age", 40);
        HeisenbergExtension config = lookupHeisenberg(HEISENBERG_EXPRESSION, event);
        HeisenbergExtension anotherConfig = lookupHeisenberg(HEISENBERG_EXPRESSION, anotherEvent);
        assertThat(config, is(not(sameInstance(anotherConfig))));
    }

    @Test
    public void initializedOptionalValueWithoutDefaultValue() throws Exception
    {
        MuleEvent event = getHeisenbergEvent();
        HeisenbergExtension config = lookupHeisenberg(HEISENBERG_EXPRESSION_BYREF, event);
        assertThat(config.getWeapon(), is(not(nullValue())));
        assertThat(config.getWeapon(), is(instanceOf(Ricin.class)));
    }

}
