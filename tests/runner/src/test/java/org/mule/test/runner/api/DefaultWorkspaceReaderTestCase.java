/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.runner.classification.DefaultWorkspaceReader.findClassPathURL;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.runner.classification.DefaultWorkspaceReader;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class DefaultWorkspaceReaderTestCase extends AbstractMuleTestCase {

  private static final String TARGET = "target";
  private static final String TEST_CLASSES = "test-classes";
  private static final String CLASSES = "classes";
  private static final String ORG_FOO_FOLDER = "org foo";
  private static final String BAR = "bar";
  private static final String TESTS_CLASSIFIER = "tests";
  private static final String JAR = "jar";
  private static final String VERSION = "1.0";
  private static final String ORG_FOO_GROUP_ID = "org.foo";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Artifact artifact;
  private File bar;
  private List<URL> urls;
  private File targetClasses;

  @Before
  public void before() throws Exception {
    artifact = new DefaultArtifact(ORG_FOO_GROUP_ID, BAR, JAR, VERSION);

    bar = new File(temporaryFolder.newFolder(ORG_FOO_FOLDER), BAR);
    if (!bar.exists()) {
      assertThat(bar.mkdir(), is(true));
    }
    setUpTargetClassesFolder();
  }

  private void setUpTargetClassesFolder() throws Exception {
    urls = newArrayList();
    targetClasses = new File(new File(bar, TARGET), CLASSES);
    assertThat(targetClasses.mkdirs(), is(true));
    urls.add(targetClasses.toURI().toURL());
  }

  @Test
  public void resolveAlsoReleaseVersions() throws Exception {
    WorkspaceLocationResolver workspaceLocationResolver = mock(WorkspaceLocationResolver.class);
    when(workspaceLocationResolver.resolvePath(artifact.getArtifactId())).thenReturn(bar);
    DefaultWorkspaceReader reader = new DefaultWorkspaceReader(urls, workspaceLocationResolver);

    File result = reader.findArtifact(artifact);
    assertThat(result, equalTo(targetClasses));
  }

  @Test
  public void handleEncodedURLs() throws Exception {
    File result = findClassPathURL(artifact, bar, urls);
    assertThat(result, equalTo(targetClasses));
  }

  @Test
  public void handleTestAndTargetClassesURLs() throws Exception {
    File targetTestClasses = new File(new File(bar, TARGET), TEST_CLASSES);
    assertThat(targetTestClasses.mkdirs(), is(true));
    urls.add(targetTestClasses.toURI().toURL());

    File result = findClassPathURL(artifact, bar, urls);
    assertThat(result, equalTo(targetClasses));

    result = findClassPathURL(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), TESTS_CLASSIFIER,
                                                  artifact.getExtension(), artifact.getVersion()),
                              bar, urls);
    assertThat(result, equalTo(targetTestClasses));
  }

}
