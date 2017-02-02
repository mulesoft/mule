/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.meta.NameableObject;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.processor.Processor;

import java.nio.charset.Charset;
import java.util.List;

/**
 * <code>Transformer</code> can be chained together to covert message payloads from one object type to another.
 */
public interface Transformer extends Processor, Initialisable, Disposable, NameableObject, MuleContextAware {

  /**
   * Determines if a particular source class can be handled by this transformer
   *
   * @param dataType The DataType to check for compatibility
   * @return true if the transformer supports this type of class or false otherwise
   * @since 3.0.0
   */
  boolean isSourceDataTypeSupported(DataType dataType);

  /**
   * Returns an unmodifiable list of Source types registered on this transformer
   *
   * @return an unmodifiable list of Source types registered on this transformer
   * @since 3.0.0
   */
  List<DataType> getSourceDataTypes();

  /**
   * Does this transformer allow null input?
   *
   * @return true if this transformer can accept null input
   */
  boolean isAcceptNull();

  /**
   * By default, Mule will throw an exception if a transformer is invoked with a source object that is not compatible with the
   * transformer. Since transformers are often chained, it is useful to be able to ignore a transformer in the chain and move to
   * the next one.
   *
   * @return true if the transformer can be ignored if the current source type is not supported, false if an exception should be
   *         throw due to an incompatible source type being passed in.
   */
  boolean isIgnoreBadInput();

  /**
   * Transforms the supplied data and returns the result
   *
   * @param src the data to transform
   * @return the transformed data
   * @throws TransformerException if a error occurs transforming the data or if the expected returnClass isn't the same as the
   *         transformed data
   */
  Object transform(Object src) throws TransformerException;

  /**
   * Transforms the supplied data and returns the result
   *
   * @param src the data to transform
   * @param encoding the encoding to use by this transformer. many transformations will not need encoding unless dealing with text
   *        so you only need to use this method if yo wish to customize the encoding
   * @return the transformed data
   * @throws TransformerException if a error occurs transforming the data or if the expected returnClass isn't the same as the
   *         transformed data
   */
  Object transform(Object src, Charset encoding) throws TransformerException;

  /**
   * Sets the expected return type for the transformed data. If the transformed data is not of this class type a
   * <code>TransformerException</code> will be thrown.
   *
   * @param type the expected return type for this transformer
   * @since 3.0.0
   */
  void setReturnDataType(DataType type);

  /**
   * Specifies the return type of the result after this transformer has been executed. Mule will use this to validate the return
   * type but also allow users to perform automatic transformations based on the source type of the object to transform and this
   * return type.
   *
   * @return the excepted return type for this transformer
   * @since 3.0.0
   */
  DataType getReturnDataType();

  @Override
  default ProcessingType getProcessingType() {
    return CPU_INTENSIVE;
  }

}
