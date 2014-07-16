/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.model;

import org.mule.api.MuleEventContext;

/**
 * <code>EntryPointResolver</code> resolves a method to call on the given
 * service object when an event is received.
 * This object maintains a set of Resolvers that will be used in order to resolve
 * an entrypoint on a service object until one is found or until the set is
 * exhausted.
 *
 * Note that there is a one-to-one mapping from component to EntryPointResolverSet.  Each component must get a separate set
 * with {@link org.mule.api.model.EntryPointResolver} instances that are not shared. An EntryPointResolver is allowed to cache state
 * and can assume the component will always be of the the same type.
 */
public interface EntryPointResolverSet
{

    /**
     * Will attempt to invoke the component by looping through all {@link org.mule.api.model.EntryPointResolver} instances registered on this set until
     * a match is found
     * @param component the component to invoke
     * @param context the current event context, this is used to figure out which method to call on the component
     * @return the result of the invocation
     * @throws Exception if the invocation itself or an {@link EntryPointResolver} fails
     */
    Object invoke(Object component, MuleEventContext context) throws Exception;

    /**
     * Will add a resolver to the list of resolvers to invoke on a compoent.
     * Implementations must maintain an ordered list of resolvers
     *
     * @param resolver the resolver to add
     */
    void addEntryPointResolver(EntryPointResolver resolver);

    /**
     * Removes a resolver from the list
     *
     * @param resolver the resolver to remove
     * @return true if the resolver was found and removed from the list
     */
    boolean removeEntryPointResolver(EntryPointResolver resolver);

}
