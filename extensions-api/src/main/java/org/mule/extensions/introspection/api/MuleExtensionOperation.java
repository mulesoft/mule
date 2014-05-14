/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import java.util.List;

public interface MuleExtensionOperation extends Described
{

    List<MuleExtensionParameter> getParameters();

    List<Class<?>> getInputTypes();

    List<Class<?>> getOutputType();

    /**
     * Returns a new {@link OperationContext}
     * with default parameters set.
     * @return
     */
    OperationContext prepare();

    boolean isAvailableFor(MuleExtensionConfiguration muleExtensionConfiguration);
}
