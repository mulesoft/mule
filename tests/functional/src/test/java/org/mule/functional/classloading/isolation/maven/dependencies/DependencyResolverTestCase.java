/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.classloading.isolation.maven.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import org.mule.functional.classloading.isolation.maven.DependenciesGraph;
import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.Sets;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DependencyResolverTestCase extends AbstractMuleTestCase
{

    private DependencyResolver builder;

    private MavenArtifact rootArtifact;
    private MavenArtifact commonsLangArtifact;
    private MavenArtifact gsonArtifact;
    private MavenArtifact commonsCliArtifact;
    private MavenArtifact dom4JArtifact;
    private MavenArtifact javaxInjectArtifact;
    private MavenArtifact junitArtifact;

    @Before
    public void setUp() throws MalformedURLException
    {
        buildDefaultArtifacts();
    }

    @Test
    public void excludeRootSelectProvidedDependenciesOnlyTestTransitiveDependencies()
    {
        builder = new DependencyResolver(new Configuration()
                                             .setMavenDependencyGraph(buildDefaultDependencies())
                                             .selectDependencies(
                                                     new DependenciesFilter()
                                                             .match(dependency -> dependency.isProvidedScope())
                                                             .onlyCollectTransitiveDependencies()
                                             )
                                             .collectTransitiveDependencies(
                                                     new TransitiveDependenciesFilter()
                                                             .match(dependency -> dependency.isTestScope())
                                                             .evaluateTransitiveDependenciesWhenPredicateFails()
                                             )
        );

        assertTrue(builder.resolveDependencies().isEmpty());
    }

    @Test
    public void collectDependenciesUsingDependencyAsRootArtifact()
    {
        ValueHolder<MavenArtifact> selectedRootArtifactHolder = new ValueHolder<>(rootArtifact);

        builder = new DependencyResolver(new Configuration()
                                                 .setMavenDependencyGraph(buildDefaultDependencies())
                                                 .includeRootArtifact(artifact -> artifact.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId()))
                                                 .selectDependencies(
                                                         new DependenciesFilter()
                                                                 .match(dependency -> dependency.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId())
                                                                                      || (rootArtifact.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId()) && dependency.isProvidedScope())))
                                                 .collectTransitiveDependencies(
                                                         new TransitiveDependenciesFilter()
                                                                 .match(transitiveDependency -> transitiveDependency.isProvidedScope())
                                                 )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(3));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
        assertThat(results.get(2), equalTo(dom4JArtifact));

        // Now we change the selectedRootArtifact to commonsCli
        selectedRootArtifactHolder.set(commonsCliArtifact);

        dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(dom4JArtifact));
    }

    @Test
    public void onlyTestTransitiveDependencies()
    {
        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .onlyCollectTransitiveDependencies()
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilter()
                                                                     .match(dependency -> dependency.isTestScope())
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(1));
        MavenArtifact mavenArtifact = dependencies.iterator().next();
        assertThat(mavenArtifact, equalTo(junitArtifact));
    }

    @Test
    public void excludeRootOnlyProvidedDependencies()
    {
        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilter()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                                     .evaluateTransitiveDependenciesWhenPredicateFails()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(dom4JArtifact));
    }

    @Test
    public void onlyProvidedDependenciesIncludingRootArtifactWithoutTransitiveDependencies()
    {
        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .includeRootArtifact()
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
    }

    @Test
    public void excludeRootOnlyCompileDependencies()
    {
        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilter()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .evaluateTransitiveDependenciesWhenPredicateFails()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsLangArtifact));
        assertThat(results.get(1), equalTo(gsonArtifact));
    }

    @Test
    public void onlyCompileDependenciesIncludingRootArtifact()
    {
        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .includeRootArtifact()
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilter()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .evaluateTransitiveDependenciesWhenPredicateFails()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(3));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsLangArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
        assertThat(results.get(2), equalTo(gsonArtifact));
    }

    @Test
    public void excludeRootOnlyProvidedAndTransitiveDependencies()
    {
        dom4JArtifact = buildMavenArtifact(dom4JArtifact.getGroupId(), dom4JArtifact.getArtifactId(), dom4JArtifact.getType(), dom4JArtifact.getVersion(), "compile");

        Set<MavenArtifact> commonsCliDependencies = new HashSet<>();
        commonsCliDependencies.add(dom4JArtifact);

        LinkedHashMap<MavenArtifact, Set<MavenArtifact>> transitiveDependencies = new LinkedHashMap<>();
        transitiveDependencies.put(commonsCliArtifact, commonsCliDependencies);

        builder = new DependencyResolver(new Configuration()
                                                     .setMavenDependencyGraph(new DependenciesGraph(rootArtifact, Sets.newHashSet(commonsCliArtifact), transitiveDependencies))
                                                     .selectDependencies(
                                                             new DependenciesFilter()
                                                                     .onlyCollectTransitiveDependencies()
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilter()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .evaluateTransitiveDependenciesWhenPredicateFails()
                                                     )
        );

        Set<MavenArtifact> results = builder.resolveDependencies();

        assertThat(results.size(), equalTo(1));
        assertThat(results.iterator().next(), equalTo(dom4JArtifact));
    }

    private List<MavenArtifact> sortArtifacts(Set<MavenArtifact> mavenArtifacts)
    {
        return mavenArtifacts.stream().sorted((a1, a2) -> a1.getArtifactId().compareTo(a2.getArtifactId())).collect(Collectors.toList());
    }

    private MavenArtifact buildMavenArtifact(String groupId, String artifactId, String type, String version, String scope)
    {
        return MavenArtifact.builder().withGroupId(groupId).withArtifactId(artifactId).withType(type).withVersion(version).withScope(scope).build();
    }

    private void buildDefaultArtifacts()
    {
        rootArtifact = buildMavenArtifact("org.my.company", "core-artifact", "jar", "1.0.0", "compile");
        commonsLangArtifact = buildMavenArtifact("org.apache.commons", "commons-lang3", "jar", "3.4", "compile");
        gsonArtifact = buildMavenArtifact("com.google.code.gson", "gson", "jar", "2.6.2", "compile");
        commonsCliArtifact = buildMavenArtifact("commons-cli", "commons-cli", "jar", "1.2", "provided");
        dom4JArtifact = buildMavenArtifact("dom4j", "dom4j", "jar", "1.6.1", "provided");
        javaxInjectArtifact = buildMavenArtifact("javax.inject", "javax.inject", "jar", "1.0", "provided");
        junitArtifact = buildMavenArtifact("junit", "junit", "jar", "4.12", "test");
    }

    private DependenciesGraph buildDefaultDependencies()
    {
        // Dependencies
        Set<MavenArtifact> dependencies = new HashSet<>();
        dependencies.add(commonsLangArtifact);
        dependencies.add(gsonArtifact);
        dependencies.add(commonsCliArtifact);

        Set<MavenArtifact> commonsCliDependencies = new HashSet<>();
        commonsCliDependencies.add(dom4JArtifact);

        Set<MavenArtifact> gsonDependencies = new HashSet<>();
        gsonDependencies.add(javaxInjectArtifact);

        Set<MavenArtifact> commonsLangDependencies = new HashSet<>();
        commonsLangDependencies.add(junitArtifact);

        LinkedHashMap<MavenArtifact, Set<MavenArtifact>> transitiveDependencies = new LinkedHashMap<>();
        transitiveDependencies.put(commonsCliArtifact, commonsCliDependencies);
        transitiveDependencies.put(gsonArtifact, gsonDependencies);
        transitiveDependencies.put(commonsLangArtifact, commonsLangDependencies);

        return new DependenciesGraph(rootArtifact, dependencies, transitiveDependencies);
    }
} 