/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mule.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.ExtensionProperties;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

/**
 * Returns the value of the {@link ExtensionProperties#CONNECTION_PARAM} parameter,
 * which is expected to have been previously set on the supplied {@link OperationContext}.
 * <p/>
 * Notice that for this to work, the {@link OperationContext}
 * has to be an instance of {@link OperationContextAdapter}
 *
 * @since 4.0
 */
public class ConnectorArgumentResolver implements ArgumentResolver<Object>
{

    /**
     * Returns the connection previously set on the {@code operationContext} under the key
     * {@link ExtensionProperties#CONNECTION_PARAM}
     *
     * @param operationContext an {@link OperationContext}
     * @return the connection
     * @throws IllegalArgumentException if the connection was not set
     * @throws ClassCastException       if {@code operationContext} is not an {@link OperationContextAdapter}
     */
    @Override
    public Object resolve(OperationContext operationContext)
    {
        Object connection = ((OperationContextAdapter) operationContext).getVariable(CONNECTION_PARAM);
        checkArgument(connection != null, "No connection was provided for the operation");

        return connection;
    }
}
