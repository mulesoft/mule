/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
