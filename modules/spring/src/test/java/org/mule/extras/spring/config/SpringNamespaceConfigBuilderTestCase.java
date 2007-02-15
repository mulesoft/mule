/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.config.ConfigurationBuilder;
import org.mule.tck.AbstractConfigBuilderTestCase;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{

    public String getConfigResources()
    {
        return "test-xml-mule2-config.xml,test-xml-mule2-config-split.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        return new Spring2ConfigurationBuilder();
    }

}