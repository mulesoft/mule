/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.internal.el.datetime.DateTime;
import org.mule.runtime.core.api.util.NetworkUtils;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class ServerContextTestCase extends AbstractELTestCase {

  public ServerContextTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Test
  public void host() throws UnknownHostException {
    Assert.assertEquals(NetworkUtils.getLocalHost().getCanonicalHostName(), evaluate("server.host"));
  }

  @Test
  public void assignValueToHost() {
    assertFinalProperty("server.host='1'");
  }

  @Test
  public void ip() throws UnknownHostException {
    Assert.assertEquals(NetworkUtils.getLocalHost().getHostAddress(), evaluate("server.ip"));
  }

  @Test
  public void assignValueToIp() {
    assertFinalProperty("server.ip='1'");
  }

  @Test
  public void javaSystemProperties() {
    Assert.assertEquals(System.getProperties(), evaluate("server.systemProperties"));
  }

  @Test
  public void assignValueToJavaSystemProperties() {
    assertFinalProperty("server.systemProperties='1'");
  }

  @Test
  public void tmpDir() {
    Assert.assertEquals(System.getProperty("java.io.tmpdir"), evaluate("server.tmpDir"));
  }

  @Test
  public void assignValueToTmpdir() {
    assertFinalProperty("server.tmpDir='1'");
  }

  @Test
  public void fileSeperator() {
    Assert.assertEquals(System.getProperty("file.separator"), evaluate("server.fileSeparator"));
  }

  @Test
  public void assignValueToFileseperator() {
    assertFinalProperty("server.fileSeparator='1'");
  }

  @Test
  public void osName() {
    Assert.assertEquals(System.getProperty("os.name"), evaluate("server.osName"));
  }

  @Test
  public void assignValueToOsName() {
    assertFinalProperty("server.osName='1'");
  }

  @Test
  public void osArch() {
    Assert.assertEquals(System.getProperty("os.arch"), evaluate("server.osArch"));
  }

  @Test
  public void assignValueToOsArch() {
    assertFinalProperty("server.osArch='1'");
  }

  @Test
  public void osVersion() {
    Assert.assertEquals(System.getProperty("os.version"), evaluate("server.osVersion"));
  }

  @Test
  public void assignValueToOsVersion() {
    assertFinalProperty("server.os.version='1'");
  }

  @Test
  public void javaVersion() {
    Assert.assertEquals(System.getProperty("java.version"), evaluate("server.javaVersion"));
  }

  @Test
  public void assignValueToJavaVersion() {
    assertFinalProperty("server.javaVersion='1'");
  }

  @Test
  public void javaVendor() {
    Assert.assertEquals(System.getProperty("java.vendor"), evaluate("server.javaVendor"));
  }

  @Test
  public void assignValueToJavaVendor() {
    assertFinalProperty("server.javaVendor='1'");
  }

  @Test
  public void env() {
    Assert.assertEquals(System.getenv(), evaluate("server.env"));
  }

  @Test
  public void assignValueToEnv() {
    assertFinalProperty("server.env='1'");
  }

  @Test
  public void timeZone() {
    Assert.assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone"));
  }

  @Test
  public void assignValueToTimeZone() {
    assertFinalProperty("server.timeZone='1'");
  }

  @Test
  public void locale() {
    Assert.assertEquals(Locale.getDefault(), evaluate("server.locale"));
  }

  @Test
  public void assignValueToLocal() {
    assertFinalProperty("server.locale='1'");
  }

  @Test
  public void userName() {
    Assert.assertEquals(System.getProperty("user.name"), evaluate("server.userName"));
  }

  @Test
  public void assignValueToUserName() {
    assertFinalProperty("server.userName='1'");
  }

  @Test
  public void userHome() {
    Assert.assertEquals(System.getProperty("user.home"), evaluate("server.userHome"));
  }

  @Test
  public void assignValueToUserHome() {
    assertFinalProperty("server.userHome='1'");
  }

  @Test
  public void userDir() {
    Assert.assertEquals(System.getProperty("user.dir"), evaluate("server.userDir"));
  }

  @Test
  public void assignValueToUserDir() {
    assertFinalProperty("server.userDir='1'");
  }

  @Test
  public void dateTime() {
    DateTime serverTime = (DateTime) evaluate("server.dateTime");
    long diff = new Date().getTime() - serverTime.toDate().getTime();
    assertThat("server.dateTime is not returning the current time", diff, lessThan(DEFAULT_TEST_TIMEOUT_SECS * 1000L));
    assertThat(evaluate("server.dateTime"), instanceOf(DateTime.class));
  }

  @Test
  public void assignValueToDateTime() {
    assertFinalProperty("server.dateTime='1'");
  }

}
