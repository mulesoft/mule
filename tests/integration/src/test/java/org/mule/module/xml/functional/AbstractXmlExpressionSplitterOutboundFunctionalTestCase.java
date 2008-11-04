/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;


public abstract class AbstractXmlExpressionSplitterOutboundFunctionalTestCase extends AbstractXmlSplitterOutboundFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-outbound-expression-functional-test.xml";
    }
}
