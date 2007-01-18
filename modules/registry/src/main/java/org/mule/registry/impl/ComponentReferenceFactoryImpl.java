/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.registry.ComponentReference;
import org.mule.registry.ComponentReferenceFactory;
import org.mule.util.StringUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ComponentReferenceFactoryImpl implements ComponentReferenceFactory
{
    private static String defaultReferenceType = REF_MULE_COMPONENT;

    public ComponentReferenceFactoryImpl()
    {
    }

    public ComponentReference getInstance()
    {
        return getInstance(defaultReferenceType);
    }

    public ComponentReference getInstance(String referenceType)
    {
        if (StringUtils.isBlank(referenceType))
        {
            return new BasicComponentReference();
        }
        else if (referenceType.equals(REF_MULE_COMPONENT))
        {
            return new BasicComponentReference();
        }
        else if (referenceType.equals(REF_OSGI_REGISTRATION))
        {
            return new OSGIServiceRegistration();
        }
        else
        {
            return new BasicComponentReference();
        }
    }
}
