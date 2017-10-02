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
import static org.mockito.Matchers.any;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URL;
import java.util.Map;

@SmallTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(MuleArtifactClassLoader.class)
public class ErrorHooksConfigurationTestCase extends AbstractMuleTestCase {

  private static String externalSetupText;

  public static void setExternalSetupText(String value) {
    externalSetupText = value;
  }

  public static final String TEST_ERROR_HOOKS_CLASS_LOCATION =
      "/org/mule/runtime/module/artifact/classloader/TestErrorHooksConfiguration.class";

  @Before
  public void setUp() throws Exception {
    mockStatic(MuleArtifactClassLoader.class);
    when(MuleArtifactClassLoader.class, "getErrorHooksClassLocation").thenReturn(TEST_ERROR_HOOKS_CLASS_LOCATION);
    externalSetupText = "notCalled";
  }

  @Test
  public void invokesErrorHooksConfiguration() throws Exception {
    when(MuleArtifactClassLoader.class, "isReactorLoaded", any(MuleArtifactClassLoader.class)).thenReturn(true);
    when(MuleArtifactClassLoader.class, "isPrivilegedApiAccessible", any(MuleArtifactClassLoader.class)).thenReturn(true);

    TestArtifactClassLoader classLoader = new TestArtifactClassLoader();
    assertThat(externalSetupText, is("called"));
  }

  @Test
  public void doesNotInvokeErrorHooksConfiguration() throws Exception {
    when(MuleArtifactClassLoader.class, "isReactorLoaded", any(MuleArtifactClassLoader.class)).thenReturn(false);

    TestArtifactClassLoader classLoader = new TestArtifactClassLoader();
    assertThat(externalSetupText, is("notCalled"));
  }

  private class TestArtifactClassLoader extends MuleArtifactClassLoader {

    TestArtifactClassLoader() throws Exception {
      super("testId", new ArtifactDescriptor("test"), new URL[] {new File("nonexistent/path").toURI().toURL()},
            currentThread().getContextClassLoader(), createLookupPoilicy());
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

