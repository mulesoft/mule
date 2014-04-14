/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import java.util.List;
import java.util.Properties;

/**
 * Allows to discover properties to be used during the bootstrap process.
 */
public interface RegistryBootstrapDiscoverer
{

    List<Properties> discover() throws Exception;
}
