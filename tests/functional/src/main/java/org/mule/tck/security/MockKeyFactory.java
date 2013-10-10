/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.security;

import org.mule.security.SecretKeyFactory;

/**
 * Empty mock for tests
 */
public class MockKeyFactory extends Named implements SecretKeyFactory
{

    public byte[] getKey()
    {
        return "key".getBytes();
    }

}
