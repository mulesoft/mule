/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.scripting;

import org.mule.tck.FunctionalTestCase;

import java.util.Properties;

public abstract class AbstractScriptingExampleTestCase extends FunctionalTestCase
{    
    //@Override
    protected String getConfigResources()
    {
        return "change-machine.xml";
    }

    abstract protected String getScriptFile();
    
    abstract protected String getCurrency();
    
    //@Override
    protected Properties getStartUpProperties()
    {
        Properties props = new Properties();
        props.put("scriptfile", getScriptFile());
        props.put("currency", getCurrency());
        return props;
    }
}
