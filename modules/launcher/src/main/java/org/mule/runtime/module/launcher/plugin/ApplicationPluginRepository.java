/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import java.io.IOException;
import java.util.List;

/**
 * Repository that defines {@link org.mule.runtime.module.launcher.application.ApplicationPlugin} bundled with the container
 * @since 4.0
 */
public interface ApplicationPluginRepository
{
    /**
     * @return a non null List of {@link ApplicationPluginDescriptor} corresponding to application plugins already bundled with the container.
     * @throws IOException if an error happens while building the descriptors from application plugins file.
     */
    List<ApplicationPluginDescriptor> getContainerApplicationPluginDescriptors() throws IOException;
}
