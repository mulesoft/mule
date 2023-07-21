/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.compression;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

/**
 * <code>AbstractCompressionTransformer</code> is a base class for all transformers that can compress or uncompress data when they
 * performa message transformation. Compression is done via a pluggable strategy.
 *
 * @deprecated No implementations remain of this class.
 */
@NoExtend
@Deprecated
public abstract class AbstractCompressionTransformer extends AbstractTransformer {

  private CompressionStrategy strategy;

  /**
   * default constructor required for discovery
   */
  public AbstractCompressionTransformer() {
    super();
  }

  public CompressionStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(CompressionStrategy strategy) {
    this.strategy = strategy;
  }

}
