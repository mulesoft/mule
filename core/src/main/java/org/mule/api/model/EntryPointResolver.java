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
