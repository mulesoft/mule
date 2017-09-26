/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.junit.Test;

import java.util.Map;

public class AppContextTestCase extends AbstractELTestCase {

  public AppContextTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Test
  public void name() {
    assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name"));
  }

  public void assignValueToName() {
    assertFinalProperty("app.name='1'");
  }

  @Test
  public void encoding() {
    assertEquals(muleContext.getConfiguration().getDefaultEncoding(), evaluate("app.encoding"));
  }

  public void assignValueToEncoding() {
    assertFinalProperty("app.encoding='1'");
  }

  @Test
  public void workDir() {
    assertEquals(muleContext.getConfiguration().getWorkingDirectory(), evaluate("app.workDir"));
  }

  public void assignValueToWorkDir() {
    assertFinalProperty("app.workDir='1'");
  }

  @Test
  public void standalone() {
    assertFalse(muleContext.getClusterId(), (Boolean) evaluate("app.standalone"));
  }

  public void assignValueToStandalone() {
    assertFinalProperty("app.standalone='1'");
  }

  @Test
  public void registryInstanceOfMap() {
    assertTrue(evaluate("app.registry") instanceof Map);
  }

  public void assignValueToRegistry() {
    assertFinalProperty("app.registy='1'");
  }

  @Test
  public void registryGet() throws RegistrationException {
    Object o = new Object();
    getRegistry().registerObject("myObject", o);
    assertEquals(o, evaluate("app.registry.myObject"));
    assertEquals(o, evaluate("app.registry['myObject']"));
  }

  @Test
  public void registryPut() throws RegistrationException {
    evaluate("app.registry.myString ='dan'");
    assertEquals("dan", getRegistry().lookupObject("myString"));
  }

  @Test
  public void registryPutAll() throws RegistrationException {
    evaluate("app.registry.putAll({'1' :'one', '2' : 'two'})");
    assertEquals("one", getRegistry().lookupObject("1"));
    assertEquals("two", getRegistry().lookupObject("2"));
  }

  @Test
  public void registryContainsKey() throws RegistrationException {
    getRegistry().registerObject("myString", "dan");
    assertTrue((Boolean) evaluate("app.registry.containsKey('myString')"));
  }

  private MuleRegistry getRegistry() {
    return ((MuleContextWithRegistries) muleContext).getRegistry();
  }

  @Test
  public void registryEntrySet() {
    assertUnsupportedOperation("app.registry.entrySet()");
  }

  @Test
  public void registryIsEmpty() {
    assertFalse((Boolean) evaluate("app.registry.isEmpty()"));
  }

  @Test
  public void registryClear() {
    assertUnsupportedOperation("app.registry.clear()");
  }

  @Test
  public void registryValues() {
    assertUnsupportedOperation("app.registry.values()");
  }

  @Test
  public void registrySize() {
    assertUnsupportedOperation("app.registry.size()");
  }

  @Test
  public void registryContainsValue() {
    assertUnsupportedOperation("app.registry.containsValue('foo')");
  }

}
