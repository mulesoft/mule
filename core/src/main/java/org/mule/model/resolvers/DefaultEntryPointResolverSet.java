/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.InvocationResult;
import org.mule.util.CollectionUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides the default implementation of an {@link org.mule.api.model.EntryPointResolverSet}
 * It resolves a method to call on the given service when an event is received.
 * This object maintains a set of Resolvers that will be used in order to resolve
 * an entrypoint on a service object until one is found or until the set is
 * exhausted.
 */
public class DefaultEntryPointResolverSet implements EntryPointResolverSet
{
    private final Set<EntryPointResolver> entryPointResolvers = new LinkedHashSet<EntryPointResolver>(4);

    public Object invoke(Object component, MuleEventContext context) throws Exception
    {
        Set<String> exceptions = new HashSet<String>();

        for (EntryPointResolver resolver : entryPointResolvers)
        {
            InvocationResult result = resolver.invoke(component, context);
            if (result.getState() == InvocationResult.State.SUCCESSFUL)
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

    /**
     * @return the entry point resolves configured in this resolver set. Note that access to the
     * set is not thread safe. Client code must take proper precautions to synchronize.
     */
    public Set<EntryPointResolver> getEntryPointResolvers()
    {
        return entryPointResolvers;
    }

    public void setEntryPointResolvers(Set<EntryPointResolver> entryPointResolvers)
    {
        synchronized (this.entryPointResolvers)
        {
            this.entryPointResolvers.clear();
            this.entryPointResolvers.addAll(entryPointResolvers);
        }
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
        synchronized (entryPointResolvers)
        {
            return this.entryPointResolvers.remove(resolver);
        }
    }
}
