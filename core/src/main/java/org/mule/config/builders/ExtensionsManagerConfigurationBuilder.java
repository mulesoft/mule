/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.extension.ExtensionManager;
import org.mule.util.ClassUtils;

/**
 * Implementation of {@link org.mule.api.config.ConfigurationBuilder}
 * that register a {@link ExtensionManager} if
 * it's present in the classpath
 *
 * @since 3.7.0
 */
public class ExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder
{

    private static final String EXTENSIONS_MANAGER_CLASS_NAME = "org.mule.module.extension.internal.manager.DefaultExtensionManager";

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        if (muleContext instanceof DefaultMuleContext &&
            ClassUtils.isClassOnPath(EXTENSIONS_MANAGER_CLASS_NAME, getClass()))
        {
            ExtensionManager extensionManager = (ExtensionManager) ClassUtils.instanciateClass(EXTENSIONS_MANAGER_CLASS_NAME);
            extensionManager.discoverExtensions(Thread.currentThread().getContextClassLoader());

            ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
            initialiseIfNeeded(extensionManager, muleContext);
        }
    }
}
