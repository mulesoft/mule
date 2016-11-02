/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.introspection.TypeIntrospecterDelegate;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import javax.wsdl.Part;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Body parts of a web service operation.
 * <p>
 * This is the base class for both INPUT and OUTPUT body resolution, the {@link TypeIntrospecterDelegate} is in charge
 * to get the information to introspect the soap parts from.
 *
 * @since 4.0
 */
final class BodyElementResolver extends NodeElementResolver {

  BodyElementResolver(TypeIntrospecterDelegate delegate) {
    super(delegate);
  }

  @Override
  public MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    WscConnection connection = getConnection(context);
    WsdlIntrospecter introspecter = connection.getWsdlIntrospecter();
    Part body = introspecter.getBodyPart(operationName, delegate)
        .orElseThrow(() -> new MetadataResolvingException(format("operation [%s] does not have a body part", operationName),
                                                          INVALID_CONFIGURATION));
    return buildPartMetadataType(connection.getTypeLoader(), body);
  }
}
