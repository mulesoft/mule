/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.InvocationResult;
import org.mule.util.CollectionUtils;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides the default implementation of an {@link org.mule.api.model.EntryPointResolverSet}
 * It resolves a method to call on the given service when an event is received.
 * This object maintains a set of Resolvers that will be used in order to resolve
 * an entrypoint on a service object until one is found or until the set is
 * exhausted.
 */
public class DefaultEntryPointResolverSet implements EntryPointResolverSet
{

    protected final Log logger = LogFactory.getLog(getClass());

    private final Set entryPointResolvers = new LinkedHashSet(4);
    private List exceptions = new CopyOnWriteArrayList();

    public Object invoke(Object component, MuleEventContext context) throws Exception
    {
        try
        {
            for (Iterator iterator = entryPointResolvers.iterator(); iterator.hasNext();)
            {
                EntryPointResolver resolver = (EntryPointResolver) iterator.next();
                InvocationResult result = resolver.invoke(component, context);
                if (result.getState() == InvocationResult.STATE_INVOKED_SUCESSFUL)
                {
                    return result.getResult();
                }
                else
                {
                    if (result.hasError())
                    {
                        exceptions.add(result.getErrorMessage());
                    }
                }
            }
            throw new EntryPointNotFoundException(CollectionUtils.toString(exceptions, true));
        }
        finally
        {
            exceptions.clear();
        }

    }

    public Set getEntryPointResolvers()
    {
        return entryPointResolvers;
    }

    public void setEntryPointResolvers(Set entryPointResolvers)
    {
        this.entryPointResolvers.clear();
        this.entryPointResolvers.addAll(entryPointResolvers);
    }

    public void addEntryPointResolver(EntryPointResolver resolver)
    {
        synchronized (entryPointResolvers)
        {
            this.entryPointResolvers.add(resolver);
        }
    }

    public boolean removeEntryPointResolver(EntryPointResolver resolver)
    {
        return this.entryPointResolvers.remove(resolver);
    }
}
