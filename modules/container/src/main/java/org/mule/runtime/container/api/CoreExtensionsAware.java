/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import java.util.List;

/**
 * Enables injection of all available {@link MuleCoreExtension}.
 * <p/>
 * Classes implementing this interface cannot contain methods annotated with {@link MuleCoreExtensionDependency} as that will
 * create an unresolvable dependency on runtime.
 */
public interface CoreExtensionsAware {

  void setCoreExtensions(List<MuleCoreExtension> coreExtensions);
}
