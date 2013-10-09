/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.model;

import org.mule.api.MuleEventContext;

/**
 * <code>EntryPointResolver</code> resolves a method to call on the given
 * Component when an event is received for the service.
 *
 * Note that one instance of an entry point will be created for a component. This means the type of the component will always be
 * of the same type for the life of the instance. Resolvers must be thread safe since multiple requests on the same component can
 * happen concurrently.  The recommended approach is to use atomic values and concurrent collections where needed rather than
 * synchronizing the invoke method, which could impact performance
 */
public interface EntryPointResolver
{
    InvocationResult invoke(Object component, MuleEventContext context) throws Exception;
}
