/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctionalTestComponentTestCase extends AbstractMuleTestCase
{
    FunctionalTestComponent ftc;
    
    @Before
    public void initFunctionaTestComponent()
    {
        ftc = new FunctionalTestComponent();
        ftc.setThrowException(true);
    }
    
    @Test
    public void defaultExceptionWithDefaultText() throws Exception
    {
        checkExceptionThrown(FunctionalTestException.class, FunctionalTestException.EXCEPTION_MESSAGE);
    }
    
    @Test
    public void defaultExceptionWithCustomText() throws Exception
    {
        String exceptionText = "BOOM";
        ftc.setExceptionText(exceptionText);
        
        checkExceptionThrown(FunctionalTestException.class, exceptionText);
    }
    
    @Test
    public void customExceptionWithoutText() throws Exception
    {
        ftc.setExceptionToThrow(IOException.class);
        checkExceptionThrown(IOException.class, null);
    }

    @Test
    public void customExceptionWithCustomText() throws Exception
    {
        String exceptionText = "BOOM";
        ftc.setExceptionToThrow(IOException.class);
        ftc.setExceptionText(exceptionText);
        checkExceptionThrown(IOException.class, exceptionText);
    }

    private void checkExceptionThrown(Class<? extends Exception> exceptionClass, String expectedMessage)
    {
        try
        {
            ftc.onCall(null);
        }
        catch (Exception e)
        {
            assertTrue(e.getClass().isAssignableFrom(exceptionClass));
            assertEquals(expectedMessage, e.getMessage());
        }
    }
}
