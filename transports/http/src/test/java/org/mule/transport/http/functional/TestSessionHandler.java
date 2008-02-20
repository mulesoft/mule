/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.transport.http.HttpSessionHandler;

/**
 * This is a custom subclass of the regular HttpSessionHandler that's used in
 * HttpServiceOverridesTestCase to see if the service override properly instantiates
 * the right class.
 */
public class TestSessionHandler extends HttpSessionHandler
{
    // no custom methods
}
