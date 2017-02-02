/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static org.mule.extension.ws.internal.util.WscMetadataTypeUtils.getAttachmentFields;
import static org.mule.extension.ws.internal.util.WscMetadataTypeUtils.getOperationType;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import org.mule.extension.ws.api.SoapAttachment;
import org.mule.extension.ws.api.SoapMessageBuilder;
import org.mule.extension.ws.internal.introspection.InputTypeIntrospecterDelegate;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

import java.util.List;

/**
 * {@link InputTypeResolver} implementation to resolve metadata for an input message of a particular operation.
 *
 * @since 4.0
 */
public class MessageBuilderResolver extends BaseWscResolver implements InputTypeResolver<String> {

  private static final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private final BodyElementResolver bodyElementResolver;
  private final HeadersElementResolver headersElementResolver;
  private final MetadataType attachmentsType;

  public MessageBuilderResolver() {
    InputTypeIntrospecterDelegate delegate = new InputTypeIntrospecterDelegate();
    bodyElementResolver = new BodyElementResolver(delegate);
    headersElementResolver = new HeadersElementResolver(delegate);
    attachmentsType = TYPE_LOADER.load(SoapAttachment.class);
  }

  @Override
  public String getResolverName() {
    return "ConsumeInputResolver";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Creates a complex object that represents the {@link SoapMessageBuilder} that contains a body, a Set of headers an a set of
   * attachments. Any component can be represented as a {@link NullType} if there is no required data for the field.
   */
  @Override
  public MetadataType getInputMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    MetadataType headersType = headersElementResolver.getMetadata(context, operationName);
    MetadataType bodyType = bodyElementResolver.getMetadata(context, operationName);

    List<ObjectFieldType> attachmentFields = getAttachmentFields(bodyType);
    MetadataType attachmentsType = getInputAttachmentsType(context.getTypeBuilder(), attachmentFields);
    MetadataType inputBodyType = getInputBodyType(bodyType, attachmentFields);

    ObjectTypeBuilder typeBuilder = context.getTypeBuilder().objectType();
    typeBuilder.addField().key(HEADERS_FIELD).value(headersType);
    typeBuilder.addField().key(BODY_FIELD).value(inputBodyType);
    typeBuilder.addField().key(ATTACHMENTS_FIELD).value(attachmentsType);
    return typeBuilder.build();
  }

  /**
   * Creates an {@link ObjectType} with a field for each required attachment for the operation.
   *
   * @param builder     a {@link BaseTypeBuilder} to create the type.
   * @param attachments the list of attachments required by the operation.
   */
  private MetadataType getInputAttachmentsType(BaseTypeBuilder builder, List<ObjectFieldType> attachments) {
    if (attachments.isEmpty()) {
      return NULL_TYPE;
    }
    ObjectTypeBuilder type = builder.objectType();
    attachments.forEach(a -> type.addField().key(getLocalPart(a)).value(attachmentsType));
    return type.build();
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
  private MetadataType getInputBodyType(MetadataType bodyType, List<ObjectFieldType> attachments) {
    if (!attachments.isEmpty() && bodyType instanceof ObjectType) {
      ObjectType operationType = getOperationType(bodyType);
      attachments.forEach(a -> operationType.getFields().removeIf(f -> getLocalPart(f).equals(getLocalPart(a))));
      if (operationType.getFields().isEmpty()) {
        return NULL_TYPE;
      }
    }
    return bodyType;
  }
}
