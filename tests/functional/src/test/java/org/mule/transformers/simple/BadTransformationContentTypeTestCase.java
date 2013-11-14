/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;

public class BadTransformationContentTypeTestCase extends FunctionalTestCase
{
    public BadTransformationContentTypeTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "bad-content-type-setting-transformer-configs.xml";
    }

    @Test(expected = BeanCreationException.class)
    public void testReturnType() throws Exception
    {
        muleContext.start();
        muleContext.getRegistry().lookupTransformer("testTransformer");
    }
}
