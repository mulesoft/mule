/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer.datatype;

import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.util.ClassUtils;

import java.util.Collection;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A serializer to handle instances of {@link DefaultCollectionDataType}.
 *
 * @since 4.0
 */

public class CollectionDataTypeXStreamConverter implements Converter {

  @Override
  public boolean canConvert(Class type) {
    return DefaultCollectionDataType.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    final DefaultCollectionDataType dataType = (DefaultCollectionDataType) source;
    writer.addAttribute("type", dataType.getType().getName());
    writer.addAttribute("mediaType", dataType.getMediaType().toRfcString());
    writer.addAttribute("itemType", dataType.getType().getName());
    writer.addAttribute("itemMediaType", dataType.getMediaType().toRfcString());
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Class<? extends Collection> type;
    Class<?> itemType;
    try {
      type = ClassUtils.getClass(reader.getAttribute("type"));
      itemType = ClassUtils.getClass(reader.getAttribute("itemType"));
    } catch (ClassNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
    final String mediaType = reader.getAttribute("mediaType");
    final String itemMediaType = reader.getAttribute("itemMediaType");
    return createDataType(type, mediaType, itemType, itemMediaType);
  }


  protected CollectionDataType createDataType(Class<? extends Collection> type, String mimeType, Class<?> itemType,
                                              String itemMediaType) {
    return (CollectionDataType) DataType.builder().collectionType(type).itemType(itemType).itemMediaType(itemMediaType)
        .mediaType(mimeType).build();
  }

}
