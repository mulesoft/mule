/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.lang.String.format;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.wsdl.Part;

/**
 * Base class for metadata resolvers that resolve dynamic metadata of XML node elements.
 *
 * @since 4.0
 */
public abstract class NodeElementResolver extends BaseWscResolver {

  private static final Map<String, MetadataType> TYPES;
  private static final MetadataType NULL_TYPE = create(XML).nullType().build();

  protected final TypeResolverDelegate delegate;

  protected NodeElementResolver(TypeResolverDelegate delegate) {
    this.delegate = delegate;
  }

  static {
    StringType stringType = create(XML).stringType().build();
    BooleanType booleanType = create(XML).booleanType().build();
    DateType dateType = create(XML).dateType().build();
    NumberType numberType = create(XML).numberType().build();
    DateTimeType dateTimeType = create(XML).dateTimeType().build();

    TYPES = ImmutableMap.<String, MetadataType>builder()
        .put("string", stringType)
        .put("boolean", booleanType)
        .put("date", dateType)
        .put("decimal", numberType)
        .put("byte", numberType)
        .put("unsignedByte", numberType)
        .put("dateTime", dateTimeType)
        .put("int", numberType)
        .put("integer", numberType)
        .put("unsignedInt", numberType)
        .put("short", numberType)
        .put("unsignedShort", numberType)
        .put("long", numberType)
        .put("unsignedLong", numberType)
        .put("double", numberType).build();
  }

  protected abstract MetadataType getMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException;

  protected MetadataType buildPartMetadataType(XmlTypeLoader loader, Part part) throws MetadataResolvingException {
    if (part.getElementName() != null) {
      String partName = part.getElementName().toString();
      return loader.load(partName)
          .orElseThrow(() -> new MetadataResolvingException(format("Could not load part element name [%s]", partName), UNKNOWN));
    }
    if (part.getTypeName() != null) {
      String localPart = part.getTypeName().getLocalPart();
      MetadataType type = TYPES.get(localPart);
      return type != null ? type : create(XML).stringType().build();
    }
    return getNullType();
  }

  protected MetadataType getNullType() {
    return NULL_TYPE;
  }
}
