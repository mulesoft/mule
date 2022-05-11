/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.CREDENTIAL_MASK;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.CUSTOM_NAMES;

import org.junit.Rule;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashMap;
import java.util.Map;

public class SplashScreenTestCase extends AbstractMuleContextTestCase {

  @Rule
  public SystemProperty statIntervalTime = new SystemProperty(CUSTOM_NAMES, "prop1,prop2");

  @Test
  public void testMuleContextSplashScreenRendering() throws Exception {
    SplashScreen serverStartupSplashScreen = new ServerStartupSplashScreen();
    assertNotNull(serverStartupSplashScreen);
    assertTrue(serverStartupSplashScreen.toString().length() > 0);

    muleContext.start();
    muleContext.stop();
    String initialStartBoilerPlate = serverStartupSplashScreen.toString();

    muleContext.start();
    muleContext.stop();
    String subsequentStartBoilerPlate = serverStartupSplashScreen.toString();

    // Only lightly validate on size because content changes, e.g. server start time-stamp
    assertEquals("Splash-screen sizes differ, ", initialStartBoilerPlate.length(), subsequentStartBoilerPlate.length());
  }

  @Test
  public void splashScreenWithMaskedProperties() throws Exception {
    SplashScreen serverStartupSplashScreen = new ServerStartupSplashScreen();
    Map<String, String> properties = new HashMap<>();
    properties.put("someProp", "someValue");
    properties.put("someKey", "somePasswordSuperSensitive");
    properties.put("key", "someKey");
    properties.put("password", "password... it shouldn't be seen");
    properties.put("anotherPassWoRD", "please, don't");
    properties.put("prop1", "nope");
    properties.put("prop2", "nope2");
    properties.put("prop12", "yes!");
    serverStartupSplashScreen.listItems(properties, "Mule properties");
    String splash = serverStartupSplashScreen.toString();
    assertThat(splash, containsString("someProp = someValue"));
    assertThat(splash, not(containsString("someKey = somePasswordSuperSensitive")));
    assertThat(splash, containsString("someKey = " + CREDENTIAL_MASK));
    assertThat(splash, not(containsString("key = someKey")));
    assertThat(splash, containsString("key = " + CREDENTIAL_MASK));
    assertThat(splash, not(containsString("password = password... it shouldn't be seen")));
    assertThat(splash, containsString("password = " + CREDENTIAL_MASK));
    assertThat(splash, not(containsString("anotherPassWoRD = please, don't")));
    assertThat(splash, containsString("anotherPassWoRD = " + CREDENTIAL_MASK));
    assertThat(splash, not(containsString("prop1 = nope")));
    assertThat(splash, containsString("prop1 = " + CREDENTIAL_MASK));
    assertThat(splash, not(containsString("prop2 = nope2")));
    assertThat(splash, containsString("prop2 = " + CREDENTIAL_MASK));
    assertThat(splash, containsString("prop12 = yes"));
  }

}
