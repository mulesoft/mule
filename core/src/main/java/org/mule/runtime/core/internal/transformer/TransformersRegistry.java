/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.util.List;

/**
 * Provides capabilities for obtaining the appropriate transformers given the from/to {@link DataType}s.
 *
 * @since 4.5
 */
public interface TransformersRegistry {

  /**
   * This method will return a list of {@link org.mule.runtime.core.api.transformer.Transformer} objects that accept the given
   * input and return the given output type of object
   * <p/>
   * All {@link Transformer}s found will have a source that is compatible with source parameter (since if a transformer can
   * transform a super type, it should be able to transform any type that extends it) and a target such that target parameter
   * isCompatibleWith() the {@link Transformer}'s one (since if we want a transformer that returns an specific type, it should
   * return exactly that type or any type that extends it.)
   *
   * @param source The desired input type for the transformer
   * @param result the desired output type for the transformer
   * @return a list of matching transformers. If there were no matches an empty list is returned.
   */
  List<Transformer> lookupTransformers(DataType source, DataType result);

  /**
   * Will find a transformer that is the closest match to the desired input and output.
   *
   * @param source The desired input type for the transformer
   * @param result the desired output type for the transformer
   * @return A transformer that exactly matches or the will accept the input and output parameters
   * @throws TransformerException will be thrown if there is more than one match
   */
  Transformer lookupTransformer(DataType source, DataType result) throws TransformerException;

  /**
   * Allows to register transformers AFTER this registry has been initialized.
   *
   * @param transformer the transformer to register
   * @throws MuleException if there was a problem registering the transformer.
   */
  void registerTransformer(Transformer transformer) throws MuleException;

}
