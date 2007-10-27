/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import java.util.Properties;


public class JXPathPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public JXPathPropertyExtractorTestCase()
    {
        super(true);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.property", "${jxpath:/endpoint}");
        return p;
    }

    protected Object getMatchMessage()
    {
        return "<endpoint>matchingEndpoint1</endpoint>";
    }

    protected Object getErrorMessage()
    {
        return "<endpoint>missingEndpoint</endpoint>";
    }

}
