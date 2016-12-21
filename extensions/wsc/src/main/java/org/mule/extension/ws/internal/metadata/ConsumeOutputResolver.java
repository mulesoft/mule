/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static org.mule.extension.ws.internal.util.WscMetadataTypeUtils.getAttachmentFields;
import static org.mule.extension.ws.internal.util.WscMetadataTypeUtils.getOperationType;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getLocalPart;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.introspection.OutputTypeIntrospecterDelegate;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.List;

/**
 * Resolves the metadata for output payload of the {@link ConsumeOperation}.
 *
 * @since 4.0
 */
public class ConsumeOutputResolver extends BaseWscResolver implements OutputTypeResolver<String> {

  private final BodyElementResolver bodyElementResolver;

  public ConsumeOutputResolver() {
    this.bodyElementResolver = new BodyElementResolver(new OutputTypeIntrospecterDelegate());
  }

  @Override
  public String getResolverName() {
    return "ConsumeOutputResolver";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataType getOutputType(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    MetadataType bodyType = bodyElementResolver.getMetadata(context, operationName);
    List<ObjectFieldType> attachmentFields = getAttachmentFields(bodyType);
    if (!attachmentFields.isEmpty()) {
      ObjectType operationType = getOperationType(bodyType);
      attachmentFields.forEach(a -> operationType.getFields().removeIf(f -> getLocalPart(f).equals(getLocalPart(a))));
      ObjectTypeBuilder typeBuilder = context.getTypeBuilder().objectType();
      typeBuilder.addField().key(BODY_FIELD).value(bodyType);
      typeBuilder.addField().key(ATTACHMENTS_FIELD).value().arrayType().of(context.getTypeBuilder().anyType());
      return typeBuilder.build();
    }
    return bodyType;
  }
}
