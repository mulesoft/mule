/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR;
import static org.mule.test.runner.api.AetherClassPathClassifier.getMuleVersion;
import static org.mule.test.runner.api.ArtifactClassificationType.APPLICATION;
import static org.mule.test.runner.api.ArtifactClassificationType.MODULE;
import static org.mule.test.runner.api.ArtifactClassificationType.PLUGIN;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleServiceModel;
import org.mule.runtime.api.deployment.meta.MuleServiceModel.MuleServiceModelBuilder;
import org.mule.runtime.api.deployment.persistence.MuleServiceModelJsonSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ArtifactDescriptorResult.class, ArtifactResult.class})
@PowerMockIgnore("javax.management.*")
@SmallTest
public class AetherClassPathClassifierTestCase extends AbstractMuleTestCase {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = none();

  private DependencyResolver dependencyResolver;
  private ArtifactClassificationTypeResolver artifactClassificationTypeResolver;
  private ClassPathClassifierContext context;
  private AetherClassPathClassifier classifier;

  private Artifact rootArtifact;
  private Artifact serviceArtifact;
  private Dependency loggingDep;
  private Dependency fooCoreDep;
  private Dependency fooToolsArtifactDep;
  private Dependency fooTestsSupportDep;
  private Dependency guavaDep;
  private Dependency derbyDriverDep;

  private Dependency fooServiceDep;
  private List<Dependency> directDependencies;

  @Before
  public void before() throws Exception {
    String muleVersion = getMuleVersion();

    this.rootArtifact = new DefaultArtifact("org.foo:foo-root:1.0-SNAPSHOT");

    this.loggingDep = new Dependency(new DefaultArtifact("org.mule.runtime:mule-module-logging:" + muleVersion), COMPILE);
    this.fooCoreDep = new Dependency(new DefaultArtifact("org.foo:foo-core:1.0-SNAPSHOT"), PROVIDED);
    this.fooToolsArtifactDep = new Dependency(new DefaultArtifact("org.foo.tools:foo-artifact:1.0-SNAPSHOT"), PROVIDED);
    this.fooTestsSupportDep = new Dependency(new DefaultArtifact("org.foo.tests:foo-tests-support:1.0-SNAPSHOT"), TEST);
    this.derbyDriverDep = new Dependency(new DefaultArtifact("org.apache.derby:derby:10.11.1.1"), TEST);
    this.guavaDep = new Dependency(new DefaultArtifact("org.google:guava:18.0"), COMPILE);
    serviceArtifact = new DefaultArtifact("org.foo:foo-service:jar:mule-service:1.0-SNAPSHOT");
    this.fooServiceDep = new Dependency(serviceArtifact, PROVIDED);

    this.dependencyResolver = mock(DependencyResolver.class);
    this.context = mock(ClassPathClassifierContext.class);
    this.artifactClassificationTypeResolver = mock(ArtifactClassificationTypeResolver.class);
    this.classifier = new AetherClassPathClassifier(dependencyResolver, artifactClassificationTypeResolver);

    when(context.getRootArtifact()).thenReturn(rootArtifact);
    this.directDependencies = newArrayList(fooCoreDep, fooToolsArtifactDep, fooTestsSupportDep, derbyDriverDep, guavaDep);
    when(dependencyResolver.getDirectDependencies(rootArtifact)).thenReturn(directDependencies);
  }

  @After
  public void after() throws Exception {
    verify(context, atLeastOnce()).getRootArtifact();
    verify(dependencyResolver, atLeastOnce()).getDirectDependencies(rootArtifact);
  }

  @Test
  public void onlyProvidedDependenciesNoTransitiveNoManageDependenciesNoFilters() throws Exception {
    Dependency compileMuleCoreDep = fooCoreDep.setScope(COMPILE);
    Dependency compileMuleArtifactDep = fooToolsArtifactDep.setScope(COMPILE);
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(MODULE);

    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    List<Dependency> managedDependencies = newArrayList(guavaDep);
    when(artifactDescriptorResult.getManagedDependencies()).thenReturn(managedDependencies);
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class))).thenReturn(artifactDescriptorResult);

    File rootArtifactFile = temporaryFolder.newFile();
    File fooCoreArtifactFile = temporaryFolder.newFile();
    File fooToolsArtifactFile = temporaryFolder.newFile();

    Artifact jarRootArtifact = rootArtifact.setFile(rootArtifactFile);
    ArtifactResult rootArtifactResult = mock(ArtifactResult.class);
    when(rootArtifactResult.getArtifact()).thenReturn(jarRootArtifact);

    when(dependencyResolver
        .resolveArtifact(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId()))))
            .thenReturn(rootArtifactResult);

    when(dependencyResolver.resolveDependencies(argThat(nullValue(Dependency.class)),
                                                (List<Dependency>) argThat(hasItems(equalTo(compileMuleCoreDep),
                                                                                    equalTo(compileMuleArtifactDep),
                                                                                    equalTo(loggingDep))),
                                                (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                argThat(instanceOf(DependencyFilter.class)),
                                                argThat(equalTo(emptyList()))))
                                                    .thenReturn(newArrayList(fooCoreArtifactFile, fooToolsArtifactFile));

    ArtifactsUrlClassification classification = classifier.classify(context);

    assertThat(classification.getTestRunnerLibUrls(), is(empty()));
    assertThat(classification.getPluginUrlClassifications(), is(empty()));
    assertThat(classification.getApplicationSharedLibUrls(), is(empty()));
    assertThat(classification.getContainerUrls(), hasSize(3));
    assertThat(classification.getContainerUrls(),
               hasItems(fooCoreArtifactFile.toURI().toURL(), fooToolsArtifactFile.toURI().toURL(),
                        rootArtifactFile.toURI().toURL()));

    verify(artifactDescriptorResult, atLeastOnce()).getManagedDependencies();
    verify(dependencyResolver, atLeastOnce()).readArtifactDescriptor(any(Artifact.class));
    verify(dependencyResolver).resolveDependencies(argThat(nullValue(Dependency.class)),
                                                   (List<Dependency>) argThat(
                                                                              hasItems(equalTo(compileMuleCoreDep),
                                                                                       equalTo(compileMuleArtifactDep),
                                                                                       equalTo(loggingDep))),
                                                   (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                   argThat(instanceOf(DependencyFilter.class)),
                                                   argThat(equalTo(emptyList())));
    verify(artifactClassificationTypeResolver).resolveArtifactClassificationType(rootArtifact);
    verify(rootArtifactResult).getArtifact();
  }

  @Test
  public void appendApplicationUrls() throws Exception {
    Dependency compileMuleCoreDep = fooCoreDep.setScope(COMPILE);
    Dependency compileMuleArtifactDep = fooToolsArtifactDep.setScope(COMPILE);
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(APPLICATION);

    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    List<Dependency> managedDependencies = newArrayList(guavaDep);
    when(artifactDescriptorResult.getManagedDependencies()).thenReturn(managedDependencies);
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class), any(List.class))).thenReturn(artifactDescriptorResult);

    artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    when(artifactDescriptorResult.getRepositories()).thenReturn(emptyList());
    when(dependencyResolver
        .readArtifactDescriptor(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId()))))
            .thenReturn(artifactDescriptorResult);

    File rootArtifactFile = temporaryFolder.newFile();
    File fooCoreArtifactFile = temporaryFolder.newFile();
    File fooToolsArtifactFile = temporaryFolder.newFile();

    Artifact jarRootArtifact = rootArtifact.setFile(rootArtifactFile);
    ArtifactResult rootArtifactResult = mock(ArtifactResult.class);
    when(rootArtifactResult.getArtifact()).thenReturn(jarRootArtifact);

    when(dependencyResolver
        .resolveArtifact(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId()))))
            .thenReturn(rootArtifactResult);

    when(dependencyResolver.resolveDependencies(argThat(nullValue(Dependency.class)),
                                                (List<Dependency>) argThat(hasItems(equalTo(compileMuleCoreDep),
                                                                                    equalTo(compileMuleArtifactDep))),
                                                (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                argThat(instanceOf(DependencyFilter.class)),
                                                argThat(equalTo(emptyList()))))
                                                    .thenReturn(newArrayList(fooCoreArtifactFile, fooToolsArtifactFile));

    URL url = temporaryFolder.newFile().toURI().toURL();
    List<URL> testRunnerUrls = newArrayList(url);
    when(context.getTestRunnerPluginUrls()).thenReturn(testRunnerUrls);

    ArtifactsUrlClassification classification = classifier.classify(context);

    assertThat(classification.getTestRunnerLibUrls(), hasSize(2));
    assertThat(classification.getTestRunnerLibUrls(), contains(rootArtifactFile.toURI().toURL(), url));
    assertThat(classification.getPluginUrlClassifications(), is(empty()));
    assertThat(classification.getApplicationSharedLibUrls(), is(empty()));
    assertThat(classification.getContainerUrls(), hasSize(2));
    assertThat(classification.getContainerUrls(),
               hasItems(fooCoreArtifactFile.toURI().toURL(), fooToolsArtifactFile.toURI().toURL()));

    verify(artifactDescriptorResult, atLeastOnce()).getManagedDependencies();
    verify(dependencyResolver, atLeastOnce()).readArtifactDescriptor(any(Artifact.class), any(List.class));
    verify(dependencyResolver).resolveDependencies(argThat(nullValue(Dependency.class)),
                                                   (List<Dependency>) argThat(
                                                                              hasItems(equalTo(compileMuleCoreDep),
                                                                                       equalTo(compileMuleArtifactDep))),
                                                   (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                   argThat(instanceOf(DependencyFilter.class)),
                                                   argThat(equalTo(emptyList())));
    verify(artifactClassificationTypeResolver).resolveArtifactClassificationType(rootArtifact);
    verify(context, atLeastOnce()).getTestRunnerPluginUrls();
    verify(rootArtifactResult).getArtifact();
  }

  @Test
  public void pluginSharedLibUrlsNotDeclaredLibraryAsDirectDependency() throws Exception {
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(PLUGIN);

    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    when(artifactDescriptorResult.getRepositories()).thenReturn(emptyList());
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class))).thenReturn(artifactDescriptorResult);

    when(context.getApplicationSharedLibCoordinates()).thenReturn(newArrayList("org.foo.tools:foo-repository"));
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(containsString("has to be declared"));
    expectedException.expectMessage(containsString(TEST));
    classifier.classify(context);
  }

  @Test
  public void pluginSharedLibUrlsInvalidCoordiantes() throws Exception {
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(PLUGIN);

    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    when(artifactDescriptorResult.getRepositories()).thenReturn(emptyList());
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class))).thenReturn(artifactDescriptorResult);

    when(context.getApplicationSharedLibCoordinates()).thenReturn(newArrayList("foo-repository"));
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString("not a valid format"));
    classifier.classify(context);
  }

  @Test
  public void pluginSharedLibUrlsNoTransitiveNoManageDependenciesNoFilters() throws Exception {
    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(APPLICATION);

    List<Dependency> directDependencies = new ArrayList<>();
    directDependencies.add(derbyDriverDep);
    when(dependencyResolver.getDirectDependencies(rootArtifact)).thenReturn(directDependencies);

    when(context.getApplicationSharedLibCoordinates())
        .thenReturn(newArrayList(derbyDriverDep.getArtifact().getGroupId() + ":" + derbyDriverDep.getArtifact().getArtifactId()));

    File rootArtifactFile = temporaryFolder.newFile();

    Artifact jarRootArtifact = rootArtifact.setFile(rootArtifactFile);
    ArtifactResult rootArtifactResult = mock(ArtifactResult.class);
    when(rootArtifactResult.getArtifact()).thenReturn(jarRootArtifact);

    when(dependencyResolver
        .resolveArtifact(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId()))))
            .thenReturn(rootArtifactResult);

    File derbyDriverFile = temporaryFolder.newFile();
    ArtifactResult artifactResult = mock(ArtifactResult.class);
    Artifact derbyDriverFatArtifact = derbyDriverDep.getArtifact().setFile(derbyDriverFile);
    when(artifactResult.getArtifact()).thenReturn(derbyDriverFatArtifact);
    when(dependencyResolver.resolveArtifact(argThat(equalTo(derbyDriverDep.getArtifact())), argThat(equalTo(emptyList()))))
        .thenReturn(artifactResult);

    ArtifactDescriptorResult defaultArtifactDescriptorResult = noManagedDependencies();

    ArtifactsUrlClassification classification = classifier.classify(context);

    assertThat(classification.getTestRunnerLibUrls(), hasSize(1));
    assertThat(classification.getTestRunnerLibUrls(), hasItem(rootArtifactFile.toURI().toURL()));
    assertThat(classification.getPluginUrlClassifications(), is(empty()));
    assertThat(classification.getApplicationSharedLibUrls(), hasSize(1));
    assertThat(classification.getApplicationSharedLibUrls(), hasItem(derbyDriverFile.toURI().toURL()));
    assertThat(classification.getContainerUrls(), is(empty()));

    verify(defaultArtifactDescriptorResult, atLeastOnce()).getManagedDependencies();
    verify(dependencyResolver, atLeastOnce()).readArtifactDescriptor(any(Artifact.class));
    verify(dependencyResolver)
        .resolveArtifact(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId())));
    verify(dependencyResolver).resolveArtifact(argThat(equalTo(derbyDriverDep.getArtifact())), argThat(equalTo(emptyList())));
    verify(artifactClassificationTypeResolver).resolveArtifactClassificationType(rootArtifact);
  }

  @Test
  public void serviceClassification() throws Exception {
    this.directDependencies =
        newArrayList(fooCoreDep, fooToolsArtifactDep, fooTestsSupportDep, derbyDriverDep, guavaDep, fooServiceDep);
    when(dependencyResolver.getDirectDependencies(rootArtifact)).thenReturn(directDependencies);

    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    List<Dependency> managedDependencies = newArrayList(guavaDep);
    when(artifactDescriptorResult.getManagedDependencies()).thenReturn(managedDependencies);
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class), any(List.class))).thenReturn(artifactDescriptorResult);

    when(dependencyResolver
        .resolveArtifact(any(Artifact.class), any(List.class)))
            .thenAnswer(invocation -> {
              Artifact artifact = (Artifact) invocation.getArguments()[0];
              artifact = artifact.setFile(temporaryFolder.newFile());
              return new ArtifactResult(new ArtifactRequest(artifact, null, null)).setArtifact(artifact);
            });

    when(artifactClassificationTypeResolver.resolveArtifactClassificationType(rootArtifact))
        .thenReturn(APPLICATION);

    File fooServiceArtifactFile = temporaryFolder.newFolder();
    File metaInfFolder = new File(fooServiceArtifactFile, META_INF);
    metaInfFolder.delete();
    metaInfFolder.mkdir();
    File descriptor = new File(metaInfFolder, MULE_ARTIFACT_JSON_DESCRIPTOR);
    MuleServiceModel muleServiceModel = new MuleServiceModelBuilder().setName("Foo").setMinMuleVersion("4.0.0")
        .withServiceProviderClassName("org.foo.ServiceProvider")
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
        .withBundleDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
        .build();
    String model = new MuleServiceModelJsonSerializer().serialize(muleServiceModel);
    FileUtils.write(descriptor, model);

    List<File> fooServiceUrls = newArrayList(fooServiceArtifactFile);
    when(dependencyResolver.resolveDependencies(argThat(equalTo(new Dependency(fooServiceDep.getArtifact(), COMPILE))),
                                                argThat(equalTo(emptyList())),
                                                argThat(equalTo(emptyList())),
                                                argThat(instanceOf(DependencyFilter.class)),
                                                argThat(equalTo(emptyList()))))
                                                    .thenReturn(fooServiceUrls);

    File rootArtifactFile = temporaryFolder.newFile();

    Artifact jarRootArtifact = rootArtifact.setFile(rootArtifactFile);
    ArtifactResult rootArtifactResult = mock(ArtifactResult.class);
    when(rootArtifactResult.getArtifact()).thenReturn(jarRootArtifact);

    when(dependencyResolver
        .resolveArtifact(argThat(new ArtifactMatcher(rootArtifact.getGroupId(), rootArtifact.getArtifactId()))))
            .thenReturn(rootArtifactResult);

    File serviceArtifactFile = temporaryFolder.newFile();
    ArtifactResult serviceArtifactResult = mock(ArtifactResult.class);
    Artifact jarServiceArtifact = serviceArtifact.setFile(serviceArtifactFile);
    when(serviceArtifactResult.getArtifact()).thenReturn(jarServiceArtifact);

    when(dependencyResolver
        .resolveArtifact(argThat(new ArtifactMatcher(serviceArtifact.getGroupId(), serviceArtifact.getArtifactId()))))
            .thenReturn(serviceArtifactResult);

    ArtifactDescriptorResult defaultArtifactDescriptorResult = noManagedDependencies();

    Dependency compileMuleCoreDep = fooCoreDep.setScope(COMPILE);
    Dependency compileMuleArtifactDep = fooToolsArtifactDep.setScope(COMPILE);
    File fooCoreArtifactFile = temporaryFolder.newFile();
    File fooToolsArtifactFile = temporaryFolder.newFile();

    when(dependencyResolver.resolveDependencies(argThat(nullValue(Dependency.class)),
                                                (List<Dependency>) and((argThat(hasItems(equalTo(compileMuleCoreDep),
                                                                                         equalTo(compileMuleArtifactDep)))),
                                                                       argThat(hasSize(3))),
                                                (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                argThat(instanceOf(DependencyFilter.class)),
                                                argThat(equalTo(emptyList()))))
                                                    .thenReturn(newArrayList(fooCoreArtifactFile, fooToolsArtifactFile));

    ArtifactsUrlClassification classification = classifier.classify(context);

    assertThat(classification.getTestRunnerLibUrls(), hasSize(1));
    assertThat(classification.getTestRunnerLibUrls(), hasItem(rootArtifactFile.toURI().toURL()));
    assertThat(classification.getPluginUrlClassifications(), is(empty()));
    assertThat(classification.getApplicationSharedLibUrls(), is(empty()));
    assertThat(classification.getContainerUrls(), hasSize(2));
    assertThat(classification.getServiceUrlClassifications(), hasSize(1));
    assertThat(classification.getServiceUrlClassifications().get(0).getUrls(), hasItem(fooServiceArtifactFile.toURI().toURL()));

    verify(defaultArtifactDescriptorResult, atLeastOnce()).getManagedDependencies();
    verify(dependencyResolver, atLeastOnce()).readArtifactDescriptor(any(Artifact.class), anyObject());
    verify(dependencyResolver, atLeastOnce())
        .resolveDependencies(argThat(equalTo(new Dependency(fooServiceDep.getArtifact(), COMPILE))),
                             argThat(equalTo(emptyList())),
                             argThat(equalTo(emptyList())),
                             argThat(instanceOf(DependencyFilter.class)),
                             argThat(equalTo(emptyList())));
    verify(artifactClassificationTypeResolver).resolveArtifactClassificationType(rootArtifact);
    verify(dependencyResolver, atLeastOnce()).resolveDependencies(argThat(nullValue(Dependency.class)),
                                                                  (List<Dependency>) and((argThat(hasItems(equalTo(compileMuleCoreDep),
                                                                                                           equalTo(compileMuleArtifactDep)))),
                                                                                         argThat(hasSize(3))),
                                                                  (List<Dependency>) argThat(hasItems(equalTo(guavaDep))),
                                                                  argThat(instanceOf(DependencyFilter.class)),
                                                                  argThat(equalTo(emptyList())));
  }

  private ArtifactDescriptorResult noManagedDependencies() throws ArtifactDescriptorException {
    ArtifactDescriptorResult artifactDescriptorResult = mock(ArtifactDescriptorResult.class);
    List<Dependency> managedDependencies = emptyList();
    when(artifactDescriptorResult.getManagedDependencies()).thenReturn(managedDependencies);
    when(dependencyResolver.readArtifactDescriptor(any(Artifact.class))).thenReturn(artifactDescriptorResult);
    return artifactDescriptorResult;
  }

  class ArtifactMatcher extends TypeSafeMatcher<Artifact> {

    private String groupId;
    private String artifactId;

    public ArtifactMatcher(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    @Override
    protected boolean matchesSafely(Artifact artifact) {
      return artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("artifact with groupId:artifactId ").appendValue(groupId + ":" + artifactId);
    }

    @Override
    protected void describeMismatchSafely(Artifact artifact, Description mismatchDescription) {
      mismatchDescription.appendText("got artifact with id ").appendValue(toId(artifact));
    }
  }

}
