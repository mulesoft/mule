/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.registry.RegistrationException;

import java.util.Map;

import org.junit.Test;

public class AppContextTestCase extends AbstractELTestCase
{

    public AppContextTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void name()
    {
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name"));
    }

    public void assignValueToName()
    {
        assertFinalProperty("app.name='1'");
    }

    @Test
    public void encoding()
    {
        assertEquals(muleContext.getConfiguration().getDefaultEncoding(), evaluate("app.encoding"));
    }

    public void assignValueToEncoding()
    {
        assertFinalProperty("app.encoding='1'");
    }

    @Test
    public void workDir()
    {
        assertEquals(muleContext.getConfiguration().getWorkingDirectory(), evaluate("app.workDir"));
    }

    public void assignValueToWorkDir()
    {
        assertFinalProperty("app.workDir='1'");
    }

    @Test
    public void standalone()
    {
        assertFalse(muleContext.getClusterId(), (Boolean) evaluate("app.standalone"));
    }

    public void assignValueToStandalone()
    {
        assertFinalProperty("app.standalone='1'");
    }

    @Test
    public void registryInstanceOfMap()
    {
        assertTrue(evaluate("app.registry") instanceof Map);
    }

    public void assignValueToRegistry()
    {
        assertFinalProperty("app.registy='1'");
    }

    @Test
    public void registryGet() throws RegistrationException
    {
        Object o = new Object();
        muleContext.getRegistry().registerObject("myObject", o);
        assertEquals(o, evaluate("app.registry.myObject"));
        assertEquals(o, evaluate("app.registry['myObject']"));
    }

    @Test
    public void registryPut() throws RegistrationException
    {
        evaluate("app.registry.myString ='dan'");
        assertEquals("dan", muleContext.getRegistry().lookupObject("myString"));
    }

    @Test
    public void registryPutAll() throws RegistrationException
    {
        evaluate("app.registry.putAll({'1' :'one', '2' : 'two'})");
        assertEquals("one", muleContext.getRegistry().lookupObject("1"));
        assertEquals("two", muleContext.getRegistry().lookupObject("2"));
    }

    @Test
    public void registryContainsKey() throws RegistrationException
    {
        muleContext.getRegistry().registerObject("myString", "dan");
        assertTrue((Boolean) evaluate("app.registry.containsKey('myString')"));
    }

    @Test
    public void registryEntrySet()
    {
        assertUnsupportedOperation("app.registry.entrySet()");
    }

    @Test
    public void registryIsEmpty()
    {
        assertFalse((Boolean) evaluate("app.registry.isEmpty()"));
    }

    @Test
    public void registryClear()
    {
        assertUnsupportedOperation("app.registry.clear()");
    }

    @Test
    public void registryValues()
    {
        assertUnsupportedOperation("app.registry.values()");
    }

    @Test
    public void registrySize()
    {
        assertUnsupportedOperation("app.registry.size()");
    }

    @Test
    public void registryContainsValue()
    {
        assertUnsupportedOperation("app.registry.containsValue('foo')");
    }

}
