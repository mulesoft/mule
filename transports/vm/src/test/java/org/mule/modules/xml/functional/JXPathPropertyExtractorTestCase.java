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

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class JXPathPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    protected String getConfigResources()
    {
        return "xml/jxpath-property-extractor-test.xml";
    }

    protected Object getMatchMessage()
    {
        return "<endpoint>name</endpoint>";
    }

    protected Object getErrorMessage()
    {
        return "<endpoint>missing</endpoint>";
    }

}
