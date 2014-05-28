/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import org.mule.extensions.api.exception.NoSuchConfigurationException;
import org.mule.extensions.api.exception.NoSuchOperationException;

import java.util.List;

public interface MuleExtension extends Described, Capable
{

    String getVersion();

    List<MuleExtensionConfiguration> getConfigurations();

    MuleExtensionConfiguration getConfiguration(String name) throws NoSuchConfigurationException;

    List<MuleExtensionOperation> getOperations();

    MuleExtensionOperation getOperation(String name) throws NoSuchOperationException;

}
