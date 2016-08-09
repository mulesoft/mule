/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.MuleServer;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.FilenameUtils;
import org.mule.runtime.core.util.JdkVersionUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.security.Permission;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class MuleServerTestCase extends AbstractMuleTestCase {

  private static String originalConfigBuilderClassName = MuleServer.getConfigBuilderClassName();

  private MuleServer muleServer;

  @After
  public void restoreOriginalConfigBuilderClassName() throws Exception {
    if (muleServer != null) {
      muleServer.shutdown();
    }
    MuleServer.setConfigBuilderClassName(originalConfigBuilderClassName);
  }

  @Test
  public void testMuleServer() throws Exception {
    muleServer = new MuleServer() {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(), muleServer.getConfigurationResources());
    assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Test
  public void testMuleServerResource() throws Exception {
    muleServer = new MuleServer("org/mule/test/spring/config1/test-xml-mule2-config.xml") {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
    assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Test
  public void testMuleServerConfigArg() throws Exception {
    muleServer = new MuleServer(new String[] {"-config", "org/mule/test/spring/config1/test-xml-mule2-config.xml"}) {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    assertEquals("org/mule/test/spring/config1/test-xml-mule2-config.xml", muleServer.getConfigurationResources());
    assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Test
  public void testMuleServerMultipleSpringConfigArgs() throws Exception {
    muleServer =
        new MuleServer(new String[] {"-config", "mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml"}) {

          @Override
          public void shutdown() {
            doShutdown();
          }
        };
    assertEquals("mule-config.xml,org/mule/test/spring/config1/test-xml-mule2-config.xml",
                 muleServer.getConfigurationResources());
    assertEquals(MuleServer.CLASSNAME_DEFAULT_CONFIG_BUILDER, MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Test
  public void testMuleServerBuilerArg() throws Exception {
    muleServer = new MuleServer(new String[] {"-builder", "org.mule.runtime.config.spring.SpringXmlConfigurationBuilder"}) {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(), muleServer.getConfigurationResources());
    assertEquals("org.mule.runtime.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Test
  public void testMuleServerSpringBuilerArg() throws Exception {
    muleServer = new MuleServer(new String[] {"-builder", "spring"}) {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    assertEquals(ClassUtils.getResource("mule-config.xml", MuleServer.class).toString(), muleServer.getConfigurationResources());
    assertEquals("org.mule.runtime.config.spring.SpringXmlConfigurationBuilder", MuleServer.getConfigBuilderClassName());
    muleServer.initialize();
  }

  @Ignore("MULE-6926: Flaky test - fails on build server")
  @Test
  public void testMuleServerAppConfig() throws Exception {
    muleServer = new MuleServer(new String[] {"-config", "mule-config.xml", "-appconfig",
        "org/mule/test/spring/config1/test-app-config.properties"}) {

      @Override
      public void shutdown() {
        doShutdown();
      }
    };
    muleServer.initialize();
    final String workingDirectory = muleServer.getMuleContext().getConfiguration().getWorkingDirectory();
    assertTrue(FilenameUtils.separatorsToUnix(workingDirectory).endsWith("/target/.appT"));
  }

  @Test(expected = ExitException.class)
  public void testMuleServerJdkVersion() {
    String javaVersion = System.setProperty("java.version", "1.5.0_12");
    try {
      try {
        JdkVersionUtils.validateJdk();
        fail("Test is invalid because the Jdk version or vendor is supposed to now be invalid");
      } catch (RuntimeException e) {
        // expected
      }
      SecurityManager manager = System.getSecurityManager();
      try {
        System.setSecurityManager(new NoExitSecurityManager());
        muleServer = new MuleServer() {

          @Override
          public void shutdown() {
            doShutdown();
          }
        };
        fail("Jdk Version is invalid");
      } finally {
        System.setSecurityManager(manager);
      }
    } finally {
      System.setProperty("java.version", javaVersion);
    }
  }

  private static final class NoExitSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
      // allow everything
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
      // allow everything
    }

    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }
  }

  private static class ExitException extends SecurityException {

    public final int status;

    public ExitException(int status) {
      super();
      this.status = status;
    }
  }

}
