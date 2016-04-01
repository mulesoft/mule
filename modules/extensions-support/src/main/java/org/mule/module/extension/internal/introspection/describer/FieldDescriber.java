/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.extension.api.introspection.declaration.fluent.ParameterizedDeclarer;

import java.lang.reflect.Field;

/**
 * A delegate object used to derived a {@link ParameterDeclarer}
 * from a {@link Field}
 *
 * @since 4.0
 */
interface FieldDescriber
{

    /**
     * Determines if {@code this} instance is capable of processing
     * the given {@code field}
     *
     * @param field a {@link Field} from a class annotated with the SDK annotations
     * @return whether or not {@code this} instance should process the field
     */
    boolean accepts(Field field);

    /**
     * Transforms the given {@code field} into a {@link ParameterDeclarer}
     *
     * @param field    the {@link Field} being processed
     * @param declarer a {@link ParameterizedDeclarer} object used to create the descriptor
     * @return a {@link ParameterDeclarer}
     */
    ParameterDeclarer describe(Field field, ParameterizedDeclarer declarer);
}
