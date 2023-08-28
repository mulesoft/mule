/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
