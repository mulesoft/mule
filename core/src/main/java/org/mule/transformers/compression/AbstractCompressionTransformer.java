/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.compression;

import org.mule.transformers.AbstractTransformer;
import org.mule.util.compression.CompressionStrategy;

/**
 * <code>AbstractCompressionTransformer</code> is a base class for all
 * transformers that can compress or uncompress data when they performa message
 * transformation. Compression is done via a pluggable strategy.
 * 
 * @author Ross Mason
 * @version $Revision$
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
