/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
