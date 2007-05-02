/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.RegistryContext;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * TODO
 */
public class PropertyPlaceholderProcessor extends PropertyPlaceholderConfigurer
{


    //@java.lang.Override
    protected Properties mergeProperties() throws IOException
    {
        Properties props = super.mergeProperties();
        Map p = RegistryContext.getRegistry().lookupProperties();
        if(p!=null)
        {
            props.putAll(p);
        }
        return props;
    }
}
