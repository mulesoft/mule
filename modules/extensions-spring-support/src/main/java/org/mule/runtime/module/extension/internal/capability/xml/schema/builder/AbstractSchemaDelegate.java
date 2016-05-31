/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for schema delegates
 *
 * @since 4.0
 */
abstract class AbstractSchemaDelegate
{

    private final Set<String> registeredElements = new HashSet<>();

    /**
     * Tracks the {@code name} of a registered entity and indicates
     * whether an element of such name has already been processed
     *
     * @param name the name of an element to be registered
     * @return whether the element has been already processed
     */
    protected boolean trackElement(String name)
    {
        return !registeredElements.add(name);
    }
}
