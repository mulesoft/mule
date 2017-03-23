/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.metadata;

import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.core.util.collection.ImmutableSetCollector;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import java.util.List;
import java.util.Set;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Body parts of a web service operation.
 *
 * @since 4.0
 */
final class KeysMetadataResolver {

  final WsdlIntrospecter introspecter;
  final TypeLoader loader;

  KeysMetadataResolver(WsdlIntrospecter introspecter, TypeLoader loader) {
    this.introspecter = introspecter;
    this.loader = loader;
  }

  public Set<MetadataKey> getMetadataKeys() throws MetadataResolvingException {
    List<String> operations = introspecter.getOperationNames();
    return operations.stream().map(ope -> newKey(ope).build()).collect(new ImmutableSetCollector<>());
  }

}
