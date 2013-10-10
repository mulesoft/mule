/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.session.SerializeAndEncodeSessionHandler;

/**
 * This is a custom subclass of the regular SerializeAndEncodeSessionHandler that's used in
 * HttpServiceOverridesTestCase to see if the service override properly instantiates
 * the right class.
 */
public class TestSessionHandler extends SerializeAndEncodeSessionHandler
{
    // no custom methods
}
