/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    private Map<String, String> namespaces = new HashMap<String, String>(2);
    private Map<String, String> configNamespaces = new HashMap<String, String>(8);

    private boolean includeConfigNamespaces = false;


    @Override
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

    public Map<String, String> getNamespaces()
    {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces)
    {
        this.namespaces = namespaces;
    }

    public Map<String, String> getConfigNamespaces()
    {
        return configNamespaces;
    }

    public void setConfigNamespaces(Map<String, String> configNamespaces)
    {
        this.configNamespaces = configNamespaces;
    }
}
