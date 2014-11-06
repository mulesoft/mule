/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.FunctionalTestCase;

import javax.xml.transform.URIResolver;

import org.junit.Test;

public class XsltTransformerWithUriResolverTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "xslt-transformer-wth-uri-resolver-config.xml";
    }

    @Test
    public void configuresUriResolver() throws Exception  {
        Transformer transformer = muleContext.getRegistry().lookupTransformer("testTransformer");
        assertTrue(transformer instanceof XsltTransformer);
        XsltTransformer xsltTransformer = (XsltTransformer) transformer;

        URIResolver uriResolver = muleContext.getRegistry().lookupObject("testResolver");
        assertEquals(uriResolver, xsltTransformer.getUriResolver());
    }
}
