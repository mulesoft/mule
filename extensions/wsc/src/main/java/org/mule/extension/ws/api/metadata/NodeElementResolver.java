/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.metadata;

import static java.lang.String.format;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.wsdl.Part;

public abstract class NodeElementResolver extends BaseWscResolver {

  private static final Map<String, MetadataType> TYPES = new LinkedHashMap<>();
  private static final MetadataType NULL_TYPE = create(XML).nullType().build();

  protected final TypeResolverDelegate delegate;

  protected NodeElementResolver(TypeResolverDelegate delegate) {
    this.delegate = delegate;
  }

  static {
    DefaultStringType stringType = create(XML).stringType().build();
    BooleanType booleanType = create(XML).booleanType().build();
    DateType dateType = create(XML).dateType().build();
    NumberType numberType = create(XML).numberType().build();
    DateTimeType dateTimeType = create(XML).dateTimeType().build();

    TYPES.put("string", stringType);
    TYPES.put("boolean", booleanType);
    TYPES.put("date", dateType);
    TYPES.put("decimal", numberType);
    TYPES.put("byte", numberType);
    TYPES.put("unsignedByte", numberType);
    TYPES.put("dateTime", dateTimeType);
    TYPES.put("int", numberType);
    TYPES.put("integer", numberType);
    TYPES.put("unsignedInt", numberType);
    TYPES.put("short", numberType);
    TYPES.put("unsignedShort", numberType);
    TYPES.put("long", numberType);
    TYPES.put("unsignedLong", numberType);
    TYPES.put("double", numberType);
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
