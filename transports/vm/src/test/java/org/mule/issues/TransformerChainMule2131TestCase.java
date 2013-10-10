/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
