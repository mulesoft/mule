/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import com.google.inject.Module;

/**
 * A factory used to create a Guice module. This is introduced by Mule so that the {@link org.mule.module.guice.GuiceConfigurationBuilder} can
 * discover module factories and allow the factory to configure the module before adding it to the Guice injector.
 * If a module does not need to perform any configuration after instanciation there is no need to create a factory for it, Mule will discover all
 * instances of {@link Module} on the classpath and instanciate them.
 */
public interface GuiceModuleFactory
{
    public Module createModule();
}
