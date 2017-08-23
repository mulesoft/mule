/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application.builder;

import org.mule.module.launcher.application.Application;

import java.io.IOException;

/**
 * Interface for building {@link Application}
 */
public interface MuleApplicationBuilder<A extends Application>
{
    A buildApplication() throws IOException;
}
