/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.functional.classloading.isolation.maven.MavenArtifactMatcherPredicate.ANY_WILDCARD;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Predicate;

import org.junit.Test;

@SmallTest
public class MavenArtifactMatcherPredicateTestCase extends AbstractMuleTestCase
{

    private final MavenArtifact muleCoreArtifact = MavenArtifact.builder().withGroupId("org.mule").withArtifactId("mule-core").withType("jar").withScope("compile").build();
    private final MavenArtifact muleCoreArtifactTest = MavenArtifact.builder().withGroupId("org.mule").withArtifactId("mule-core").withType("test-jar").withScope("compile").build();
    private final MavenArtifact muleValidationArtifact = MavenArtifact.builder().withGroupId("org.mule").withArtifactId("mule-validations").withType("jar").withScope("compile").build();
    private final MavenArtifact muleValidationTransportArtifact = MavenArtifact.builder().withGroupId("org.mule.transports").withArtifactId("mule-validations").withType("jar").withScope("compile").build();
    private final MavenArtifact commonsCollections = MavenArtifact.builder().withGroupId("commons-collections").withArtifactId("commons-collections").withType("jar").withScope("compile").build();

    @Test
    public void matchByGroupId()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule", ANY_WILDCARD, ANY_WILDCARD);

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));
        assertThat(predicate.test(muleCoreArtifactTest), equalTo(true));
        assertThat(predicate.test(muleValidationArtifact), equalTo(true));

        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(false));
        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

    @Test
    public void matchByGroupIdStartsWith()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule*", ANY_WILDCARD, ANY_WILDCARD);

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));
        assertThat(predicate.test(muleCoreArtifactTest), equalTo(true));
        assertThat(predicate.test(muleValidationArtifact), equalTo(true));
        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(true));

        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

    @Test
    public void matchByArtifactId()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule", "mule-core", ANY_WILDCARD);

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));
        assertThat(predicate.test(muleCoreArtifactTest), equalTo(true));

        assertThat(predicate.test(muleValidationArtifact), equalTo(false));
        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(false));
        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

    @Test
    public void matchByArtifactIdStartsWith()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule", "mule-" + ANY_WILDCARD, ANY_WILDCARD);

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));
        assertThat(predicate.test(muleCoreArtifactTest), equalTo(true));
        assertThat(predicate.test(muleValidationArtifact), equalTo(true));

        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(false));
        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

    @Test
    public void matchByType()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule", "mule-core", "jar");

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));

        assertThat(predicate.test(muleCoreArtifactTest), equalTo(false));
        assertThat(predicate.test(muleValidationArtifact), equalTo(false));
        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(false));
        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

    @Test
    public void orMatching()
    {
        Predicate<MavenArtifact> predicate = new MavenArtifactMatcherPredicate("org.mule", "mule-core", "jar").or(new MavenArtifactMatcherPredicate("org.mule.transports", ANY_WILDCARD, ANY_WILDCARD));

        assertThat(predicate.test(muleCoreArtifact), equalTo(true));

        assertThat(predicate.test(muleCoreArtifactTest), equalTo(false));
        assertThat(predicate.test(muleValidationArtifact), equalTo(false));

        assertThat(predicate.test(muleValidationTransportArtifact), equalTo(true));

        assertThat(predicate.test(commonsCollections), equalTo(false));
    }

}
