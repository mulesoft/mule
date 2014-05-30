/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.spi;

import org.mule.extensions.introspection.api.MuleExtensionParameter;

public interface MuleExtensionParameterBuilder extends Builder<MuleExtensionParameter>
{

    MuleExtensionParameterBuilder setName(String name);

    MuleExtensionParameterBuilder setDescription(String description);

    MuleExtensionParameterBuilder setType(Class<?> type);

    MuleExtensionParameterBuilder setRequired(boolean required);

    MuleExtensionParameterBuilder setDynamic(boolean dynamic);

    MuleExtensionParameterBuilder setDefaultValue(Object defaultValue);

}
