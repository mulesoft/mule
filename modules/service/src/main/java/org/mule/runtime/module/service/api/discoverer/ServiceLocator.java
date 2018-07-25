/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

public interface ServiceLocator {

  String getName();

  ServiceProvider getServiceProvider();

  ArtifactClassLoader getClassLoader();

  Class<? extends Service> getServiceContract();

}
