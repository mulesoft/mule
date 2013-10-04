/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.junit.Test;

public class TransformerChainMule2131TestCase extends TransformerChainMule2063TestCase
{

    public TransformerChainMule2131TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testOutputTransformers() throws Exception
    {
        doTest("test3", TEST3_OUT);
    }

}
