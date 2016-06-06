/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import static java.lang.String.format;

import org.mule.runtime.api.metadata.MetadataKey;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} implementation for {@link MetadataKey}
 *
 * @since 4.0
 */
final public class MetadataKeyMatcher extends TypeSafeMatcher<MetadataKey>
{

    private final String id;
    private String displayName;
    private String partName;

    private MetadataKeyMatcher(String id)
    {

        this.id = id;
    }

    /**
     * Creates a new instance of the {@link MetadataKeyMatcher}
     *
     * @param id of the {@link MetadataKey}
     * @return the new instance of {@link MetadataKeyMatcher}
     */
    public static MetadataKeyMatcher metadataKeyWithId(String id)
    {
        return new MetadataKeyMatcher(id);
    }

    @Override
    protected boolean matchesSafely(MetadataKey metadataKey)
    {

        if (!metadataKey.getId().equals(id))
        {
            return false;
        }

        if (displayName != null && !metadataKey.getDisplayName().equals(displayName))
        {
            return false;
        }

        if (partName != null && !metadataKey.getPartName().equals(partName))
        {
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText(format("a MetadataKey with id: '%s'", id));
        if (displayName != null)
        {
            description.appendText(format(", displayName: '%s'", displayName));
        }
        if (partName != null)
        {
            description.appendText(format(", partName: '%s'", partName));
        }
    }

    /**
     * Adds a displayName to compare. If is not added the matcher won't compare displayNames
     *
     * @param displayName of the {@link MetadataKey}
     * @return the contributed {@link MetadataKeyMatcher}
     */
    public MetadataKeyMatcher withDisplayName(String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    /**
     * Adds a partName to compare. If is not added the matcher won't compare partNames
     *
     * @param partName of the {@link MetadataKey}
     * @return the contributed {@link MetadataKeyMatcher}
     */
    public MetadataKeyMatcher withPartName(String partName)
    {
        this.partName = partName;
        return this;
    }
}
