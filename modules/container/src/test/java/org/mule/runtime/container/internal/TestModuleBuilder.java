/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Builds instances of {@link MuleModule}.
 * <p/>
 * Builder instances are not reusable. Creaet a new one every time a module must be created.
 */
public class TestModuleBuilder
{

    private final String name;
    private Set<String> packages = new HashSet<>();
    private Set<String> paths = new HashSet<>();

    /**
     * Creates a new builder
     *
     * @param name name for the module. Not empty
     */
    public TestModuleBuilder(String name)
    {
        checkArgument(!StringUtils.isEmpty(name), "Name cannot be empty");
        this.name = name;
    }

    /**
     * Adds new java packages to be exported
     *
     * @param packages packages to export
     * @return {@code this}
     */
    public TestModuleBuilder exportingPackages(String... packages)
    {
        for (String packageName : packages)
        {
            this.packages.add(packageName);
        }

        return this;
    }

    /**
     * Adds new resource paths to be exported
     *
     * @param paths paths to export
     * @return {@code this}
     */
    public TestModuleBuilder exportingPaths(String... paths)
    {
        for (String path : paths)
        {
            this.paths.add(path);
        }

        return this;
    }

    /**
     * Creates a module with the configured state
     *
     * @return a new {@link MuleModule} with the configured state.
     */
    public MuleModule build()
    {
        return new MuleModule(name, packages, paths);
    }
}
