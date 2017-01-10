/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.util;

import static java.util.stream.Collectors.toList;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isObjectType;
import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;

import java.util.Collection;
import java.util.List;


/**
 * Utility class for handling XML {@link MetadataType}s on the {@link WebServiceConsumer}.
 *
 * @since 4.0
 */
public class WscMetadataTypeUtils {

  /**
   * No accessible constructor.
   */
  private WscMetadataTypeUtils() {}

  public static ObjectType getOperationType(MetadataType bodyType) {
    if (isObjectType(bodyType)) {
      Collection<ObjectFieldType> bodyFields = ((ObjectType) bodyType).getFields();
      if (bodyFields.size() == 1) {
        // Contains only one field which represents de operation
        return (ObjectType) bodyFields.iterator().next().getValue();
      }
    }
    throw new IllegalArgumentException("Could not find soap operation element in the provided body MetadataType");
  }

  public static List<ObjectFieldType> getAttachmentFields(MetadataType bodyType) {
    Collection<ObjectFieldType> operationParams = getOperationType(bodyType).getFields();
    return operationParams.stream().filter(field -> field.getValue() instanceof BinaryType).collect(toList());
  }
}
