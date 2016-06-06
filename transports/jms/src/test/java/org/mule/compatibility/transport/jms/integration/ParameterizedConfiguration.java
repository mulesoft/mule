/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import org.mule.runtime.core.api.NamedObject;

import java.util.Map;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * Parameterized Mule test classes (JUnit 4) should return a collection of objects 
 * which implement this interface in the method annotated by @Parameters
 * 
 * @see Parameterized
 * @see Parameters
 */
public interface ParameterizedConfiguration extends NamedObject
{
    /**
     * Perform any needed initialization in this method, such as loading properties from a properties file.
     * 
     * @param callingClass is sometimes needed for correct classpath ordering
     * @throws Exception
     */
    void initialise(Class callingClass) throws Exception;

    /**
     * A configuration which is not enabled will be skipped over when running tests.
     */
    boolean isEnabled();

    /**
     * Any properties returned by this method will be made available for substitution in the XML 
     * configuration file(s) for this test case.
     */
    Map getProperties();
}
