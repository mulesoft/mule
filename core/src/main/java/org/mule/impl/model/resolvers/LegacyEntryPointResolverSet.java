/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

/**
 * An {@link UMOEntryPointResolverSet} that mimics the behaviour of the Mule 1.x
 * DynamicEntryPointResolver.
 */
public class LegacyEntryPointResolverSet extends DefaultEntryPointResolverSet
{
    public LegacyEntryPointResolverSet()
    {
        addEntryPointResolver(new MethodHeaderPropertyEntryPointResolver());
        addEntryPointResolver(new CallableEntryPointResolver());

        ReflectionEntryPointResolver preTransformResolver = new ReflectionEntryPointResolver();
        //In Mule 1.x you could call setXX methods as service methods by default
        preTransformResolver.removeIgnorredMethod("set*");
        addEntryPointResolver(preTransformResolver);

        ReflectionEntryPointResolver postTransformResolver = new ReflectionEntryPointResolver();
        postTransformResolver.setTransformFirst(false);
        //In Mule 1.x you could call setXX methods as service methods by default
        postTransformResolver.removeIgnorredMethod("set*");
        addEntryPointResolver(postTransformResolver);
    }
}
