/*
 * $Id: ExpressionTransformerTestCase.java 18265 2010-07-19 09:42:45Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;

import java.util.Arrays;
import java.util.List;

public class BadTransformationContentTypeTestCase extends FunctionalTestCase
{


    public BadTransformationContentTypeTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "bad-content-type-setting-transformer-configs.xml";
    }

    public void testReturnType() throws Exception
    {
        try
        {
            muleContext.start();
            Transformer trans = muleContext.getRegistry().lookupTransformer("testTransformer");
            fail("config should fail with bad content type");
        }
        catch (Exception ex)
        {
            System.err.println("Caught " + ex.getClass() + " (as expected)");
        }

    }
}