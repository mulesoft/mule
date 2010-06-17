/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.io.IOException;

/**
 * Discovers application descriptor and settings.
 */
public interface AppBloodhound
{
    ApplicationDescriptor fetch(String appName) throws IOException;
}
