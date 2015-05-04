/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleMessage;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

/**
 * An implementation of {@link ArgumentResolver} which
 * returns the {@link MuleMessage} associated with a given
 * {@link OperationContext}.
 *
 * Notice that for this to work, the {@link OperationContext}
 * has to be an instance of {@link OperationContextAdapter}
 *
 * @since 3.7.0
 */
public class MessageArgumentResolver implements ArgumentResolver<MuleMessage>
{

    @Override
    public MuleMessage resolve(OperationContext operationContext)
    {
        return ((OperationContextAdapter) operationContext).getEvent().getMessage();
    }
}
