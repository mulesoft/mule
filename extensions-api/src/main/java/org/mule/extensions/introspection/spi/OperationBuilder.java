/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.spi;

import org.mule.extensions.introspection.api.ExtensionOperation;

public interface OperationBuilder<T extends ExtensionOperation, B> extends Builder<T>
{

    B setName(String name);

    B setDescription(String description);

    B setInputTypes(Class<?>... inputTypes);

    B setOutputTypes(Class<?>... outputTypes);

    B addParameter(MuleExtensionParameterBuilder parameter);

    B addOwnerConfiguration(String name);
}
