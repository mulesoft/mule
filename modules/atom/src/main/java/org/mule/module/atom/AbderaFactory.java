/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
