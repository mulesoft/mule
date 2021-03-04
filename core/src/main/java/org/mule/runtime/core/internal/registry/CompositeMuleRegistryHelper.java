/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;

/**
 * Composes {@link MuleRegistryHelper} of application that includes the {@link MuleRegistryHelper} of a domain.
 * 
 * @since 4.1.4
 */
public class CompositeMuleRegistryHelper extends MuleRegistryHelper {

  private MuleRegistryHelper parentRegistryHelper;

  public CompositeMuleRegistryHelper(Registry muleRegistry, MuleContext muleContext,
                                     MuleRegistryHelper parentMuleRegistryHelper) {
    super(muleRegistry, muleContext);
    this.parentRegistryHelper = parentMuleRegistryHelper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transformer lookupTransformer(DataType source, DataType result) throws TransformerException {
    Transformer transformer;
    try {
      transformer = super.lookupTransformer(source, result);
    } catch (TransformerException e) {
      transformer = parentRegistryHelper.lookupTransformer(source, result);
    }

    return transformer;
  }

}
