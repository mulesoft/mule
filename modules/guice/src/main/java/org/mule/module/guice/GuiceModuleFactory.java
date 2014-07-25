/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import com.google.inject.Module;

/**
 * A factory class that allows non-discoverable modules to be discovered and created via a factory
 *
 * @deprecated Guice module is deprecated and will be removed in Mule 4.
 */
@Deprecated
public interface GuiceModuleFactory
{
    Module createModule();
}
