/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Test;

public class HeisenbergDefaultConfigTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-default-config.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Test
    public void usesDefaultConfig() throws Exception
    {
        assertThat(getPayloadAsString(runFlow("sayMyName").getMessage()), is("Heisenberg"));
    }
}
