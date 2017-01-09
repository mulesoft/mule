/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.message.Message;

/**
 * Utility methods for creating {@link MetadataType} instances which
 * represent specific things
 *
 * @since 4.0
 */
public final class TypesFactory {

  private TypesFactory() {}

  /**
   * Creates an {@link ObjectType} which represents a {@link Message} which payload
   * is of type {@code outputType} and its attributes are of type {@code attributesType}
   *
   * @param typeBuilder    the type builder to use
   * @param outputType     the type of the payload
   * @param attributesType the type of the attributes
   * @return an {@link ObjectType}
   */
  public static ObjectType buildMessageType(BaseTypeBuilder typeBuilder,
                                            MetadataType outputType,
                                            MetadataType attributesType) {

    ObjectTypeBuilder messageType = typeBuilder.objectType().id(Message.class.getName());
    messageType.addField().key("payload").required(true).value(outputType);
    messageType.addField().key("attributes").required(true).value(attributesType);

    return messageType.build();
  }
}
