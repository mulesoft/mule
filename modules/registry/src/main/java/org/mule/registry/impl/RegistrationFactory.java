/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.registry.Registration;
import org.mule.util.StringUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class RegistrationFactory 
{
    public static String REF_MULE_COMPONENT = "mule-component";
    public static String REF_OSGI_REGISTRATION = "osgi-registration";
    private static String defaultReferenceType = REF_MULE_COMPONENT;

    public RegistrationFactory()
    {
    }

    public Registration getInstance()
    {
        return getInstance(defaultReferenceType);
    }

    public Registration getInstance(String referenceType)
    {
        if (StringUtils.isBlank(referenceType))
        {
            return new MuleRegistration();
        }
        else if (referenceType.equals(REF_MULE_COMPONENT))
        {
            return new MuleRegistration();
        }
        else if (referenceType.equals(REF_OSGI_REGISTRATION))
        {
            return new OSGIServiceRegistration();
        }
        else
        {
            return new MuleRegistration();
        }
    }
}
