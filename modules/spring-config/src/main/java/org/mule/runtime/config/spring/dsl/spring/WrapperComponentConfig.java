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
 *
 * @since 4.0
 */
public class WrapperComponentConfig
{

    private WrapperType wrapperType;
    private Optional<Class<? extends Collection>> collectionTypeOptional;

    public enum WrapperType
    {
        SINGLE, COLLECTION
    }

    public WrapperComponentConfig(WrapperType wrapperType, Optional<Class<? extends Collection>> collectionTypeOptional)
    {
        this.wrapperType = wrapperType;
        this.collectionTypeOptional = collectionTypeOptional;
    }

    public WrapperType getWrapperType()
    {
        return wrapperType;
    }

    public Optional<Class<? extends Collection>> getCollectionTypeOptional()
    {
        return collectionTypeOptional;
    }
}
