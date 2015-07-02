/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.module.extension.HeisenbergExtension.EXTENSION_NAME;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HeisenbergDefaultConfigTestCase extends ExtensionsFunctionalTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-default-config.xml";
    }

    @Test
    public void usesDefaultConfig() throws Exception
    {
        assertThat(runFlow("sayMyName").getMessage().getPayloadAsString(), is("Heisenberg"));
    }

    @Test
    public void twoConfigsAndNoConfigRef() throws Exception
    {
        ExtensionManager extensionManager = muleContext.getExtensionManager();
        Extension extension = extensionManager.getExtensions().iterator().next();
        assertThat(extension.getName(), is(EXTENSION_NAME));

        ConfigurationInstanceProvider<Object> configurationInstanceProvider = mock(ConfigurationInstanceProvider.class);
        extensionManager.registerConfigurationInstanceProvider(extension, "secondConfig", configurationInstanceProvider);

        expectedException.expectCause(IsInstanceOf.<IllegalStateException>instanceOf(IllegalStateException.class));
        runFlow("sayMyName");
    }
}
