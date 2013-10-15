/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.config.bootstrap.BootstrapObjectFactory;

import org.apache.abdera.Abdera;

/**
 * Adds an instance of the factory to the registry so that folks can have the {@link org.apache.abdera.factory.Factory} injected into their components
 */
public class AbderaFactory implements BootstrapObjectFactory
{
    public Object create()
    {
        return Abdera.getInstance().getFactory();
    }
}
