/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
