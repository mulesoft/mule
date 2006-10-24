/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

/**
 * Represents a Component type in the registry
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentType
{

    public static final ComponentType MULE_COMPONENT = new ComponentType("mule.engine",
        "A Mule component type");
    public static final ComponentType MULE_CONNECTOR_COMPONENT = new ComponentType("mule.binding",
        "A Mule connector type");
    public static final ComponentType MULE_TRANSFORMER_COMPONENT = new ComponentType("mule.transformer",
        "A Mule transformer type");
    public static final ComponentType MULE_AGENT_COMPONENT = new ComponentType("mule.agent",
        "A Mule Agent type");
    public static final ComponentType JBI_ENGINE_COMPONENT = new ComponentType("jbi.engine",
        "A JBI engine component type");
    public static final ComponentType JBI_BINDING_COMPONENT = new ComponentType("jbi.binding",
        "A JBI binding component type");

    public static final ComponentType[] COMPONENT_TYPES = new ComponentType[]{MULE_COMPONENT, MULE_COMPONENT,
        MULE_TRANSFORMER_COMPONENT, MULE_AGENT_COMPONENT, JBI_ENGINE_COMPONENT, JBI_BINDING_COMPONENT};

    private String name;
    private String description;

    public ComponentType(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ComponentType))
        {
            return false;
        }

        final ComponentType componentType = (ComponentType)o;

        if (name != null ? !name.equals(componentType.name) : componentType.name != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return (name != null ? name.hashCode() : 0);
    }

    public String toString()
    {
        return name;
    }
}
