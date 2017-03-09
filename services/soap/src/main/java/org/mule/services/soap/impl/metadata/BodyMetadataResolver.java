/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.metadata;

import static java.lang.String.format;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.services.soap.impl.util.SoapServiceMetadataTypeUtils.getAttachmentFields;
import static org.mule.services.soap.impl.util.SoapServiceMetadataTypeUtils.getOperationType;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.services.soap.impl.introspection.WsdlIntrospecter;

import java.util.List;

import javax.wsdl.Part;

/**
 * Handles the dynamic {@link MetadataType} resolution for the SOAP Body parts of a web service operation.
 *
 * @since 4.0
 */
final class BodyMetadataResolver extends NodeMetadataResolver {

  BodyMetadataResolver(WsdlIntrospecter introspecter, TypeLoader loader) {
    super(introspecter, loader);
  }

  @Override
  public MetadataType getMetadata(String operation, TypeIntrospecterDelegate delegate) throws MetadataResolvingException {
    Part bodyPart = introspecter.getBodyPart(operation, delegate)
        .orElseThrow(() -> new MetadataResolvingException(format("operation [%s] does not have a body part", operation),
                                                          INVALID_CONFIGURATION));

    MetadataType bodyType = buildPartMetadataType(bodyPart);
    List<ObjectFieldType> attachmentFields = getAttachmentFields(bodyType);
    return filterAttachmentsFromBodyType(bodyType, attachmentFields);
  }

  /**
   * Filter the attachments fields from the body metadata type since SOAP manages the attachments as regular parameters but
   * we wan't to provide a body decoupled experience for the attachments.
   * <p>
   * If after removing the attachments there are not fields remaining in the request, a {@link NullType} is returned.
   *
   * @param bodyType    the {@link MetadataType} of the xml input body, with all the required parameters including the
   * @param attachments the attachments fields on found in the type.
   * @return the body {@link MetadataType} without the attachment fields.
   */
  private MetadataType filterAttachmentsFromBodyType(MetadataType bodyType, List<ObjectFieldType> attachments) {
    if (!attachments.isEmpty() && bodyType instanceof ObjectType) {
      ObjectType operationType = getOperationType(bodyType);
      attachments.forEach(a -> operationType.getFields().removeIf(f -> getLocalPart(f).equals(getLocalPart(a))));
      if (operationType.getFields().isEmpty()) {
        return nullType;
      }
    }
    return bodyType;
  }
}
