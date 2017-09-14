/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.container.api.MuleCoreExtension;

import java.util.List;

/**
 * Defines a way to search for available {@link MuleCoreExtension}
 */
public interface MuleCoreExtensionDiscoverer {

  List<MuleCoreExtension> discover() throws MuleException;
}
