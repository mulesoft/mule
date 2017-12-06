/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.resulthandler;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isCollection;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.toDataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.util.Preconditions;

import java.util.Collection;

/**
 * {@link ReturnHandler} implementation for collections
 * <p>
 * When creates the {@link Message.Builder} it configures the correct {@link DataType} basing on the original
 * {@link MetadataType}
 *
 * @since 4.1
 */
public final class CollectionReturnHandler implements ReturnHandler<Collection> {

  private final CollectionDataType dataType;

  public CollectionReturnHandler(MetadataType outputType) {
    Preconditions.checkArgument(isCollection(outputType), "The output type is not a collection");
    dataType = (CollectionDataType) toDataType(outputType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Message.Builder toMessageBuilder(Collection value) {
    return Message.builder().collectionValue(value, dataType.getItemDataType().getType());
  }
}
