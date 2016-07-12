/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.function.Predicate;

/**
 * {@link Predicate} to match a {@link MavenArtifact} based on groupId, artifactId and type.
 * It support wildcard for any of groutId/artifactId/type fields. It is also supported partial wildcard for startsWith for groupId and artifactId.
 *
 * @since 4.0
 */
public class MavenArtifactMatcherPredicate implements Predicate<MavenArtifact>
{

    private String groupId;
    private String artifactId;
    private String type;

    public static final String ANY_WILDCARD = "*";

    /**
     * Creates a {@link Predicate} to match artifacts by groupId, artifactId and type.
     * <p/>
     * It supports startsWith wildcards for any of these fields or any (using {@link MavenArtifactMatcherPredicate#ANY_WILDCARD}).
     * <p/>
     * For instance: org.mule.*
     *
     * @param groupId non-null and cannot be empty, groupId for exclusion
     * @param artifactId non-null and cannot be empty, artifactId for exclusion
     * @param type non-null and cannot be empty, type for exclusion
     * @throws IllegalArgumentException if any of the passed arguments is null or empty
     */
    public MavenArtifactMatcherPredicate(String groupId, String artifactId, String type)
    {
        checkNullOrEmpty(groupId, "groupId");
        checkNullOrEmpty(artifactId, "artifactId");
        checkNullOrEmpty(type, "type");

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
    }

    /**
     * Checks if the value is null or empty
     *
     * @param value the {@link String} to be checked
     * @param param the name of the parameter to be set
     * @throws IllegalArgumentException if the object is null or empty
     */
    private void checkNullOrEmpty(String value, String param) throws IllegalArgumentException
    {
        if (isEmpty(value))
        {
            throw new IllegalArgumentException(param + " pattern or value cannot be empty or null");
        }
    }

    /**
     * @param mavenArtifact artifact to be evaluated
     * @return true if the {@link MavenArtifact} matches with this this predicate
     */
    @Override
    public boolean test(MavenArtifact mavenArtifact)
    {
        if (groupId.equals(ANY_WILDCARD) || groupId.equals(mavenArtifact.getGroupId()) || startsWith(groupId, mavenArtifact.getGroupId()))
        {
            if (artifactId.equals(ANY_WILDCARD) || artifactId.equals(mavenArtifact.getArtifactId()) || startsWith(artifactId, mavenArtifact.getArtifactId()))
            {
                if (type == null)
                {
                    return true;
                }
                else
                {
                    if (type.equals(ANY_WILDCARD) || type.equals(mavenArtifact.getType()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean startsWith(String pattern, String value)
    {
        if (!pattern.endsWith(ANY_WILDCARD))
        {
            return false;
        }
        return value.startsWith(pattern.split("\\*")[0]);
    }

    @Override
    public String toString()
    {
        return this.getClass().getName() + "(" + groupId + ":" + artifactId + ":" + type + ")";
    }
}
