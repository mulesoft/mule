/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.MuleServer;
import org.mule.api.config.MuleProperties;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class EmptyApplicationDescriptorTestCase extends AbstractMuleTestCase
{

    public static final String APP_NAME = "test-app";
    public static final String MULE_HOME_DIR = "home";
    @Rule
    public SystemProperty muleHome = new SystemProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, MULE_HOME_DIR);

    @Test
    public void defaultValuesAreCorrect() throws IOException
    {
        EmptyApplicationDescriptor applicationDescriptor = new EmptyApplicationDescriptor(APP_NAME);
        assertThat(applicationDescriptor.getAppName(), is(APP_NAME));
        assertThat(applicationDescriptor.getConfigResources()[0], is(MuleServer.DEFAULT_CONFIGURATION));
        String absolutePathForConfigResource = MuleContainerBootstrapUtils.getMuleAppDefaultConfigFile(APP_NAME).getAbsolutePath();
        assertThat(applicationDescriptor.getAbsoluteResourcePaths()[0], is(absolutePathForConfigResource));
        assertThat(applicationDescriptor.getConfigResourcesFile()[0].getAbsolutePath(), is(absolutePathForConfigResource));
        assertThat(applicationDescriptor.getLogConfigFile(), is(nullValue()));
    }

}
