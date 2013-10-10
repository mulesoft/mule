/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;


import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractXmlFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    public AbstractXmlFunctionalTestCase(ConfigVariant variant,
			String configResources) {
		super(variant, configResources);
	}

	public static final long TIMEOUT = 3000L;

    protected String getConfigAsString() throws IOException
    {
        return getResourceAsString(getConfigResources());
    }

    protected String getResourceAsString(String resource) throws IOException
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertNotNull(resource, is);
        return IOUtils.toString(is);
    }
}
