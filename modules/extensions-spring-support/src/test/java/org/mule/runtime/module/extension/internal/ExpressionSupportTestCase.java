/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExpressionSupportTestCase extends ExtensionFunctionalTestCase
{

    @Parameters(name = "{0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {"heisenberg-invalid-expression-parameter.xml"},
                {"heisenberg-fixed-parameter-with-expression.xml"}
        });
    }

    private final String config;

    public ExpressionSupportTestCase(String config)
    {
        this.config = config;
    }

    @Rule
    public ExpectedException expectedException = none();

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {config};
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        expectedException.expect(InitialisationException.class);
        expectedException.expectCause(instanceOf(IllegalArgumentException.class));
    }

    @Test
    public void invalidConfig() throws Exception
    {
        fail("Configuration should have been invalid");
    }
}
