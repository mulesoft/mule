/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.module.artifact.classloader.ChildFirstLookupStrategy.CHILD_FIRST;

import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class ErrorHooksConfigurationTestCase extends AbstractMuleTestCase {

  private static String externalSetupText;

  public static void setExternalSetupText(String value) {
    externalSetupText = value;
  }

  public static final String TEST_ERROR_HOOKS_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/TestErrorHooksConfiguration.class";

  @Test
  public void invokesErrorHooksConfiguration() throws Exception {
    TestArtifactClassLoader classLoader = new TestArtifactClassLoader() {
      @Override
      protected Boolean isReactorLoaded() {
        return true;
      }
    };

    externalSetupText = "notCalled";
    classLoader.setErrorHooksClassLocation(TEST_ERROR_HOOKS_CLASS_LOCATION);
    classLoader.configureErrorHooks();
    assertThat(externalSetupText, is("called"));
  }

  @Test
  public void doesNotInvokeErrorHooksConfiguration() throws Exception {
    TestArtifactClassLoader classLoader = new TestArtifactClassLoader() {
      @Override
      protected Boolean isReactorLoaded() {
        return false;
      }
    };

    externalSetupText = "notCalled";
    classLoader.setErrorHooksClassLocation(TEST_ERROR_HOOKS_CLASS_LOCATION);
    classLoader.configureErrorHooks();
    assertThat(externalSetupText, is("notCalled"));
  }

  private class TestArtifactClassLoader extends MuleArtifactClassLoader {
    TestArtifactClassLoader() throws Exception {
      super("testId", new ArtifactDescriptor("test"), new URL[] {new File("nonexistent/path").toURI().toURL()}, currentThread().getContextClassLoader(), createLookupPoilicy());
    }
  }

  private static ClassLoaderLookupPolicy createLookupPoilicy() {
    return new ClassLoaderLookupPolicy() {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        return CHILD_FIRST;
      }

      @Override
      public LookupStrategy getPackageLookupStrategy(String packageName) {
        return null;
      }

      @Override
      public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
        return null;
      }
    };
  }
}

