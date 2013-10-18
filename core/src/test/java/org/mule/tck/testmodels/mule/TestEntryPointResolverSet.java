/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEventContext;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;

/**
 * <code>TestEntryPointResolver</code> is a test EntryPointResolver that doesn't provide
 * any functionality. It is usually used to test confguration options
 */
public class TestEntryPointResolverSet implements EntryPointResolverSet
{
    private String testProperty;

    /**
     * Will add a resolver to the list of resolvers to invoke on a compoent.
     * Implementations must maintain an ordered list of resolvers
     *
     * @param resolver the resolver to add
     */
    public void addEntryPointResolver(EntryPointResolver resolver)
    {

    }

    public Object invoke(Object component, MuleEventContext context) throws Exception
    {
        return null;
    }

    /**
     * Removes a resolver from the list
     *
     * @param resolver the resolver to remove
     * @return true if the resolver was found and removed from the list
     */
    public boolean removeEntryPointResolver(EntryPointResolver resolver)
    {
        return false;
    }

    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }
}
