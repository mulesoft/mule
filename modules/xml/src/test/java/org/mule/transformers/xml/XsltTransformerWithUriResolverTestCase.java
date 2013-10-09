/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.xml;

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
    protected String getConfigResources()
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
