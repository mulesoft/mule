/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.xml.XmlTypeLoader.XML;
import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;

/**
 * Base class for all metadata resolvers of the {@link ConsumeOperation}.
 *
 * @since 4.0
 */
public abstract class BaseWscResolver implements NamedTypeResolver {

  static final MetadataType NULL_TYPE = create(XML).nullType().build();
  private static final String WSC_CATEGORY = "WebServiceConsumerCategory";
  public static final String BODY_FIELD = "body";
  public static final String HEADERS_FIELD = "headers";
  public static final String ATTACHMENTS_FIELD = "attachments";



  @Override
  public String getCategoryName() {
    return WSC_CATEGORY;
  }

  protected WscConnection getConnection(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return context.<WscConnection>getConnection()
        .orElseThrow(() -> new MetadataResolvingException("Could not obtain connection to retrieve metadata",
                                                          CONNECTION_FAILURE));
  }
}
