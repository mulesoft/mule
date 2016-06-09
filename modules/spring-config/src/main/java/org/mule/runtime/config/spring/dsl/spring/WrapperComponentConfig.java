/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import java.util.Collection;
import java.util.Optional;

/**
 * Holder for wrapper element configuration configuration
 */
public class WrapperComponentConfig
{

    private ChildType childType;
    private Optional<Class<? extends Collection>> collectionTypeOptional;

    public enum ChildType
    {
        SINGLE, COLLECTION
    }

    public WrapperComponentConfig(ChildType childType, Optional<Class<? extends Collection>> collectionTypeOptional)
    {
        this.childType = childType;
        this.collectionTypeOptional = collectionTypeOptional;
    }

    public ChildType getChildType()
    {
        return childType;
    }

    public Optional<Class<? extends Collection>> getCollectionTypeOptional()
    {
        return collectionTypeOptional;
    }
}
