/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformer.datatype;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.metadata.SimpleDataType;
import org.mule.runtime.core.util.ClassUtils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A serializer to handle instances of {@link SimpleDataType}.
 *
 * @since 4.0
 */
public class SimpleDataTypeXStreamConverter implements Converter {

  @Override
  public boolean canConvert(Class type) {
    return SimpleDataType.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    final SimpleDataType dataType = (SimpleDataType) source;
    writer.addAttribute("type", dataType.getType().getName());
    writer.addAttribute("mediaType", dataType.getMediaType().toRfcString());
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Class<?> type;
    try {
      type = ClassUtils.getClass(reader.getAttribute("type"));
    } catch (ClassNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
    final String mediaType = reader.getAttribute("mediaType");
    return createDataType(type, mediaType);
  }

  protected SimpleDataType createDataType(Class<?> type, String mimeType) {
    return (SimpleDataType) DataType.builder().type(type).mediaType(mimeType).build();
  }

}
