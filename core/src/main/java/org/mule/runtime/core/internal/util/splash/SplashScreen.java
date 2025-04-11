/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public static Logger LOGGER = LoggerFactory.getLogger(SplashScreen.class);

  public static final String RUNTIME_VERBOSE = SYSTEM_PROPERTY_PREFIX + "runtime.verbose";
  public static final String CUSTOM_NAMES = SYSTEM_PROPERTY_PREFIX + "splash.masked.properties";

  private static final List<String> CREDENTIAL_NAMES = asList("key", "password", "pswd");
  private static final Set<String> CUSTOM_CREDENTIAL_NAMES = new HashSet<>(asList(getProperty(CUSTOM_NAMES, "").split(",")));
  public static final String CREDENTIAL_MASK = "*****";
  /**
   * Determines whether extra information should be display.
   */
  protected static PropertyChecker RUNTIME_VERBOSE_PROPERTY = new PropertyChecker(RUNTIME_VERBOSE, TRUE.toString());

  protected static final String VALUE_FORMAT = " - %s";
  private static final String KEY_VALUE_FORMAT = VALUE_FORMAT + " = %s";
  protected List<String> header = new ArrayList<>();
  protected List<String> body = new ArrayList<>();
  protected List<String> footer = new ArrayList<>();

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
    return StringMessageUtils.getBoilerPlate(message, '+', 80);
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
        doBody(format(VALUE_FORMAT, item));
      }
    }
  }

  private boolean isCredentialItem(String key) {
    if (CUSTOM_CREDENTIAL_NAMES.contains(key)) {
      return true;
    }
    for (String credentialName : CREDENTIAL_NAMES) {
      if (key.toLowerCase().contains(credentialName)) {
        return true;
      }
    }
    return false;
  }

  protected void listItems(Map<String, String> map, String description) {
    if (!map.isEmpty()) {
      doBody(description);
      for (String key : map.keySet()) {
        String value = isCredentialItem(key) ? CREDENTIAL_MASK : map.get(key);
        doBody(format(KEY_VALUE_FORMAT, key, value));
      }
    }
  }

  @Override
  public String toString() {
    List<String> boilerPlate = new ArrayList<>(header);
    boilerPlate.addAll(body);
    boilerPlate.addAll(footer);
    return getBoilerPlate(boilerPlate, '*', 70);
  }

  protected SplashScreen() {
    // make sure no one else creates an instance
  }
}
