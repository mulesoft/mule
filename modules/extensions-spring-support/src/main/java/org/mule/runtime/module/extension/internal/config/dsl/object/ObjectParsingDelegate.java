/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.dsl.api.component.AttributeDefinition;

/**
 * Specialization of {@link ParsingDelegate} that only accepts instances represented by a {@link ObjectType} and produces
 * instances of instances of {@link AttributeDefinition.Builder}
 *
 * @since 4.0
 */
public interface ObjectParsingDelegate extends ParsingDelegate<ObjectType, AttributeDefinition.Builder> {
}
