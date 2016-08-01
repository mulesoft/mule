/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.functional.junit4.InvalidExtensionConfigTestCase;
import org.mule.test.vegan.extension.VeganExtension;

public class ConfigLevelSourceNegativeTestCase extends InvalidExtensionConfigTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {VeganExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "vegan-invalid-config-for-sources.xml";
    }
}
