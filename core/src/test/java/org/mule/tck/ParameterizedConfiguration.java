/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import org.mule.api.NamedObject;

import java.util.Map;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


/**
 * Parameterized Mule test classes (JUnit 4) should return a collection of objects 
 * which implement this interface in the method annotated by @Parameters
 * 
 * @see MuleParameterized
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
    public void initialise(Class callingClass) throws Exception;

    /**
     * A configuration which is not enabled will be skipped over when running tests.
     */
    public boolean isEnabled();

    /**
     * Any properties returned by this method will be made available for substitution in the XML 
     * configuration file(s) for this test case.
     */
    public Map getProperties();
}
