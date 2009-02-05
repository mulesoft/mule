/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple manager that holds a registry of global namespaces that will be recognised by all xml queries and transforms
 */
public class NamespaceManager implements Initialisable
{

    private Map namespaces = new HashMap(2);
    private Map configNamespaces = new HashMap(8);

    private boolean includeConfigNamespaces = false;


    public void initialise() throws InitialisationException
    {
        if (isIncludeConfigNamespaces())
        {
            namespaces.putAll(configNamespaces);
        }
    }

    public boolean isIncludeConfigNamespaces()
    {
        return includeConfigNamespaces;
    }

    public void setIncludeConfigNamespaces(boolean includeConfigNamespaces)
    {
        this.includeConfigNamespaces = includeConfigNamespaces;
    }

    public Map getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map namespaces)
    {
        this.namespaces = namespaces;
    }

    public Map getConfigNamespaces()
    {
        return configNamespaces;
    }

    public void setConfigNamespaces(Map configNamespaces)
    {
        this.configNamespaces = configNamespaces;
    }
}
