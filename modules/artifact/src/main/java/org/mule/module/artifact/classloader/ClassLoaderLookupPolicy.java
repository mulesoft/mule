/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang.ClassUtils.getPackageName;
import static org.mule.util.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Set;

/**
 * Defines which resources in a class loader should be looked up
 * using parent-first, child-first or child only methods.
 * <p/>
 * Default lookup method is parent first. To use child-first, the
 * corresponding package must be added as an overridden. To use
 * child-only, the corresponding package must be added as blocked.
 */
public class ClassLoaderLookupPolicy
{

    public static final ClassLoaderLookupPolicy NULL_LOOKUP_POLICY = new ClassLoaderLookupPolicy(emptySet(), emptySet());

    private final Set<String> overridden;
    private final Set<String> blocked;

    /**
     * Creates a new lookup policy based on the provided configuration.
     *
     * @param overridden packages that must use child first lookup method. Non null
     * @param blocked packages that must use child only lookup method. Non null
     */
    public ClassLoaderLookupPolicy(Set<String> overridden, Set<String> blocked)
    {
        checkArgument(overridden != null, "Overridden packages cannot be null");
        checkArgument(blocked != null, "Blocked packages cannot be null");

        this.overridden = Collections.unmodifiableSet(overridden);
        this.blocked = Collections.unmodifiableSet(blocked);
    }

    /**
     * @return true if the given class name is defined as overridden, false otherwise
     */
    public boolean isOverridden(String className)
    {
        final String packageName = getPackageName(className);
        return overridden.contains(packageName);
    }

    /**
     * @return true if the given package name is defined as blocked, false otherwise
     */
    public boolean isBlocked(String className)
    {
        final String packageName = getPackageName(className);
        return blocked.contains(packageName);
    }
}
