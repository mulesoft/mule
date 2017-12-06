/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.resulthandler;

import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MapDataType;
import org.mule.runtime.api.util.Preconditions;

import java.util.Map;

/**
 * {@link ReturnHandler} implementation for maps.
 * <p>
 * When creates the {@link Message.Builder} it configures the correct {@link DataType} basing on the original
 * {@link MetadataType}
 *
 * @since 4.1
 */
public final class MapReturnHandler implements ReturnHandler<Map> {

  private final Class<?> mapKeyType;
  private final Class<?> mapValueType;

  public MapReturnHandler(HasOutputModel hasOutputModel) {
    MetadataType type = hasOutputModel.getOutput().getType();
    Preconditions.checkArgument(isMap(type), "The given output type is not a Map");
    MapDataType mapDataType = (MapDataType) toDataType(type);
    mapKeyType = mapDataType.getKeyDataType().getType();
    mapValueType = mapDataType.getValueDataType().getType();
  }

  /**
   * {@inheritDoc}
   */
  public Message.Builder toMessageBuilder(Map value) {
    return Message.builder().mapValue(value, mapKeyType, mapValueType);
  }
}
