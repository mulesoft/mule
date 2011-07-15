/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
