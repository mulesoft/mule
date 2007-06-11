/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.testmodels;

import java.net.URL;
import java.util.Map;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceFactory;

public class MockServiceFactory implements ServiceFactory
{
    public Service create(Class clazz)
    {
        return null;
    }

    public Service create(Class clazz, Map properties)
    {
        return null;
    }

    public Service create(Class clazz, String name, String namespace, Map properties)
    {
        return null;
    }

    public Service create(Class clazz, QName service, URL wsdlUrl, Map properties)
    {
        return null;
    }

    public Service create(Class clazz, QName service, Definition def, Map properties)
    {
        return null;
    }
}


