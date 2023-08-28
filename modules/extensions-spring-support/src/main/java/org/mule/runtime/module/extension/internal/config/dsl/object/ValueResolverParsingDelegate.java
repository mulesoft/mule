/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * Specialization of {@link ParsingDelegate} that always produces instances of {@link ValueResolver}
 *
 * @since 4.0
 */
public interface ValueResolverParsingDelegate extends ParsingDelegate<MetadataType, ValueResolver<Object>> {

}
