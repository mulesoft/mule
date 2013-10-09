/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
