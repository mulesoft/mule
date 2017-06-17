/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class DefaultNativeLibraryFinderFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder muleHomeFolder = new TemporaryFolder();

  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();

  @Test
  public void createsPerAppNativeLibraryFinderWhenPropertyIsFalse() throws Exception {
    doCreateNativeLibraryFinderTest(ArtifactCopyNativeLibraryFinder.class);
  }

  private void doCreateNativeLibraryFinderTest(final Class<? extends NativeLibraryFinder> expectedNativeLibraryFinderClass)
      throws Exception {
    testWithSystemProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHomeFolder.getRoot().getAbsolutePath(),
                           () -> {
                             NativeLibraryFinder nativeLibraryFinder =
                                 nativeLibraryFinderFactory.create("testApp", new URL[0]);

                             assertThat(nativeLibraryFinder,
                                        instanceOf(expectedNativeLibraryFinderClass));
                           });
  }
}
