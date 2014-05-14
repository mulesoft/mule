/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.api.MuleExtensionsManager;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.util.Preconditions;

import com.google.common.collect.ForwardingIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final class DefaultMuleExtensionsManager implements MuleExtensionsManager
{

    private final List<MuleExtension> extensions = new LinkedList<MuleExtension>();

    @Override
    public void register(MuleExtension extension)
    {
        Preconditions.checkArgument(extension != null, "Cannot register a null extension");
        extensions.add(extension);
    }


    @Override
    public Iterator<MuleExtension> getExtensions()
    {
        final Iterator<MuleExtension> iterator = new ArrayList(extensions).iterator();

        return new ForwardingIterator<MuleExtension>()
        {
            @Override
            protected Iterator<MuleExtension> delegate()
            {
                return iterator;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Extensions cannot be unregistered");
            }
        };
    }
}
