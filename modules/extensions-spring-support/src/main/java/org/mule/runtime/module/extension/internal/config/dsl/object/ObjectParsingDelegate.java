/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;

/**
 * A delegate for producing a {@link AttributeDefinition.Builder} that
 * represents an {@link ObjectType}.
 *
 * @since 4.0
 */
public interface ObjectParsingDelegate
{
    /**
     * @param objectType an {@link ObjectType}
     * @return whether {@code this} instance can be used to parse the given {@code objectType}
     */
    boolean accepts(ObjectType objectType);

    /**
     * Parses the given {@code objectType}
     *
     * @param name       the element name
     * @param objectType a {@link ObjectType}
     * @return a {@link AttributeDefinition.Builder}
     */
    AttributeDefinition.Builder parse(String name, ObjectType objectType);
}
