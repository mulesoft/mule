/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.compression;

import org.mule.transformer.AbstractTransformer;
import org.mule.util.compression.CompressionStrategy;

/**
 * <code>AbstractCompressionTransformer</code> is a base class for all transformers
 * that can compress or uncompress data when they performa message transformation.
 * Compression is done via a pluggable strategy.
 */

public abstract class AbstractCompressionTransformer extends AbstractTransformer
{
    private CompressionStrategy strategy;

    /**
     * default constructor required for discovery
     */
    public AbstractCompressionTransformer()
    {
        super();
    }

    public CompressionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(CompressionStrategy strategy)
    {
        this.strategy = strategy;
    }

}
