/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionOperationGroup;

final class ImmutableMuleExtensionOperationGroup extends AbstractImmutableDescribed implements MuleExtensionOperationGroup
{

    private final AllowedChildsType allowedChildsType;
    private final int minOperations;
    private final int maxOperations;

    ImmutableMuleExtensionOperationGroup(String name,
                                         String description,
                                         AllowedChildsType allowedChildsType,
                                         int minOperations,
                                         int maxOperations)
    {
        super(name, description);
        this.allowedChildsType = allowedChildsType;
        this.minOperations = minOperations;
        this.maxOperations = maxOperations;
    }

    @Override
    public AllowedChildsType getAllowedChildsType()
    {
        return allowedChildsType;
    }

    @Override
    public int getMinOperations()
    {
        return minOperations;
    }

    @Override
    public int getMaxOperations()
    {
        return maxOperations;
    }
}
