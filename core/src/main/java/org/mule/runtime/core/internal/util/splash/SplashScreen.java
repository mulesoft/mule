/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static java.lang.Boolean.TRUE;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implements singleton pattern to allow different splash-screen implementations following the concept of header, body, and
 * footer. Header and footer are reserved internally to Mule but body can be used to customize splash-screen output. External code
 * can e.g. hook into the start-up splash-screen as follows:
 * 
 * <pre>
 * <code>
 *   SplashScreen splashScreen = SplashScreen.getInstance(ServerStartupSplashScreen.class);
 *   splashScreen.addBody("Some extra text");
 * </code>
 * </pre>
 */
public abstract class SplashScreen {

  public static final String RUNTIME_VERBOSE = SYSTEM_PROPERTY_PREFIX + "runtime.verbose";
  /**
   * Determines whether extra information should be display.
   */
  protected static PropertyChecker RUNTIME_VERBOSE_PROPERTY = new PropertyChecker(RUNTIME_VERBOSE, TRUE.toString());

  protected static final String VALUE_FORMAT = " - %s";
  private static final String KEY_VALUE_FORMAT = VALUE_FORMAT + " = %s";
  protected List<String> header = new ArrayList<String>();
  protected List<String> body = new ArrayList<String>();
  protected List<String> footer = new ArrayList<String>();

  /**
   * Setting the header clears body and footer assuming a new splash-screen is built.
   * 
   */
  final public void setHeader(MuleContext context) {
    header.clear();
    doHeader(context);
  }

  final public void addBody(String line) {
    doBody(line);
  }

  final public void setFooter(MuleContext context) {
    footer.clear();
    doFooter(context);
  }

  public static String miniSplash(final String message) {
    // middle dot char
    return StringMessageUtils.getBoilerPlate(message, '+', 60);
  }

  protected void doHeader(MuleContext context) {
    // default reserved for mule core info
  }

  protected void doBody(String line) {
    body.add(line);
  }

  protected void doFooter(MuleContext context) {
    // default reserved for mule core info
  }

  protected void listItems(Collection<String> items, String description) {
    if (!items.isEmpty()) {
      doBody(description);
      for (String item : items) {
        doBody(String.format(VALUE_FORMAT, item));
      }
    }
  }

  protected void listItems(Map<String, String> map, String description) {
    if (!map.isEmpty()) {
      doBody(description);
      for (String key : map.keySet()) {
        doBody(String.format(KEY_VALUE_FORMAT, key, map.get(key)));
      }
    }
  }

  public String toString() {
    List<String> boilerPlate = new ArrayList<String>(header);
    boilerPlate.addAll(body);
    boilerPlate.addAll(footer);
    return StringMessageUtils.getBoilerPlate(boilerPlate, '*', 70);
  }

  protected SplashScreen() {
    // make sure no one else creates an instance
  }
}
