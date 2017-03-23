/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.metadata;

import static java.lang.String.format;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import javax.wsdl.Part;

/**
 * Base class for metadata resolvers that resolve dynamic metadata of XML node elements.
 *
 * @since 4.0
 */
abstract class NodeMetadataResolver {

  final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(XML);
  final MetadataType nullType = typeBuilder.nullType().build();
  final WsdlIntrospecter introspecter;
  final TypeLoader loader;

  NodeMetadataResolver(WsdlIntrospecter introspecter, TypeLoader loader) {
    this.introspecter = introspecter;
    this.loader = loader;
  }

  /**
   * Resolves the metadata for an operation, Input or Output is fetched depending on the {@link TypeIntrospecterDelegate} passed
   * as parameter.
   *
   * @param operation the name of the operation that the types are going to be resolved.
   * @param delegate  a delegate that introspects the message to get the types from (input or output).
   * @throws MetadataResolvingException in any error case.
   */
  abstract MetadataType getMetadata(String operation, TypeIntrospecterDelegate delegate) throws MetadataResolvingException;

  MetadataType buildPartMetadataType(Part part) throws MetadataResolvingException {
    if (part.getElementName() != null) {
      String partName = part.getElementName().toString();
      return loader.load(partName)
          .orElseThrow(() -> new MetadataResolvingException(format("Could not load part element name [%s]", partName), UNKNOWN));
    }
    throw new MetadataResolvingException("Trying to resolve metadata for a nameless part, probably the provided WSDL is invalid",
                                         INVALID_CONFIGURATION);
  }
}
