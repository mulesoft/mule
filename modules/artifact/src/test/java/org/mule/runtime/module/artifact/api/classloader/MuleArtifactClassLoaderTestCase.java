/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class MuleArtifactClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String API_RESOURCE_NAME = "test-api.raml";
  private final URL apiLocation;
  private final URL apiResourceLocation;

  public MuleArtifactClassLoaderTestCase() throws MalformedURLException {
    apiLocation = ClassUtils.getResource("com/organization/test-artifact/1.0.0/classloader-test-api.zip", this.getClass());
    apiResourceLocation = new URL("jar:" + apiLocation.toString() + "!/" + API_RESOURCE_NAME);
  }

  @Test
  public void findsResourceFromSpecificInnerArtifact() {
    MuleArtifactClassLoader classLoader =
        new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class), new URL[] {apiLocation}, null,
                                    mock(ClassLoaderLookupPolicy.class));
    URL resource = classLoader.findResource("resource::com.organization:test-artifact:1.0.0:test-api.raml");
    assertThat(resource, is(notNullValue()));
    assertThat(resource, is(equalTo(apiResourceLocation)));
  }

  @Test
  public void findsResourceFromSpecificInnerArtifactWithoutVersion() {
    MuleArtifactClassLoader classLoader =
        new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class), new URL[] {apiLocation}, null,
                                    mock(ClassLoaderLookupPolicy.class));
    URL resource = classLoader.findResource("resource::com.organization:test-artifact::test-api.raml");
    assertThat(resource, is(notNullValue()));
    assertThat(resource, is(equalTo(apiResourceLocation)));
  }

  @Test
  public void cannotFindResourceFromSpecificInnerArtifactWithWrongVersion() {
    MuleArtifactClassLoader classLoader =
        new MuleArtifactClassLoader("test", mock(ArtifactDescriptor.class), new URL[] {apiLocation}, null,
                                    mock(ClassLoaderLookupPolicy.class));
    assertThat(classLoader.findResource("resource::com.organization:test-artifact:1.3:test-api.raml"), is(nullValue()));
  }

}
