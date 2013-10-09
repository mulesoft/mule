/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.object;

import java.util.Map;

/**
 * Creates a new instance of the object on each call.
 */
public class PrototypeObjectFactory extends AbstractObjectFactory
{

    /** For Spring only */
    public PrototypeObjectFactory()
    {
        super();
    }

    public PrototypeObjectFactory(String objectClassName)
    {
        super(objectClassName);
    }

    public PrototypeObjectFactory(String objectClassName, Map properties)
    {
        super(objectClassName, properties);
    }

    public PrototypeObjectFactory(Class<?> objectClass)
    {
        super(objectClass);
    }

    public PrototypeObjectFactory(Class<?> objectClass, Map properties)
    {
        super(objectClass, properties);
    }

}
