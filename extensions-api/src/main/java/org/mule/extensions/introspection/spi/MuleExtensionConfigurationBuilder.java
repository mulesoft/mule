/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.spi;

import org.mule.extensions.introspection.api.MuleExtensionConfiguration;

public interface MuleExtensionConfigurationBuilder extends Builder<MuleExtensionConfiguration>
{

    MuleExtensionConfigurationBuilder setName(String name);

    MuleExtensionConfigurationBuilder setDescription(String description);

    MuleExtensionConfigurationBuilder addParameter(MuleExtensionParameterBuilder parameter);

}
