/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import org.mule.MuleCoreExtension;
import org.mule.api.DefaultMuleException;

import java.util.List;

/**
 * Defines a way to search for available {@link org.mule.MuleCoreExtension}
 */
public interface MuleCoreExtensionDiscoverer
{

    List<MuleCoreExtension> discover() throws DefaultMuleException;
}
