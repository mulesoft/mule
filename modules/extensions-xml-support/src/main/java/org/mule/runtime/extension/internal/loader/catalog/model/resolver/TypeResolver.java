/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.model.resolver;

import org.mule.metadata.api.model.MetadataType;

import java.util.Optional;

/**
 * Object to return a {@link MetadataType} from an identifier, where the current identifier might be part of the current {@link TypeResolver}
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public interface TypeResolver {

  Optional<MetadataType> resolveType(String typeIdentifier);
}

