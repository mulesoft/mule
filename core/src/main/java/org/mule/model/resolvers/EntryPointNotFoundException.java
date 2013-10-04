/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;

/**
 * Tis exception gets thrown by the {@link org.mule.model.resolvers.DefaultEntryPointResolverSet} if after trying
 * all entrypointResolvers it cannot fin the entrypoint on the service service
 */
public class EntryPointNotFoundException extends MuleException
{
    /** @param message the exception message */
    public EntryPointNotFoundException(String message)
    {
        super(CoreMessages.failedToFindEntrypointForComponent(message));
    }
}
