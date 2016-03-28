/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.builder;

import static org.mule.module.launcher.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.util.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Creates Mule Domain files.
 */
public class DomainFileBuilder extends AbstractArtifactFileBuilder<DomainFileBuilder>
{

    private List<ApplicationFileBuilder> applications = new LinkedList<>();

    /**
     * Creates a new builder
     *
     * @param id artifact identifier. Non empty.
     */
    public DomainFileBuilder(String id)
    {
        super(id);
    }


    /**
     * Creates a new builder from another instance.
     *
     * @param source instance used as template to build the new one. Non null.
     */
    public DomainFileBuilder(DomainFileBuilder source)
    {
        super(source);
    }

    /**
     * Create a new builder from another instance and different ID.
     *
     * @param id artifact identifier. Non empty.
     * @param source instance used as template to build the new one. Non null.
     */
    public DomainFileBuilder(String id, DomainFileBuilder source)
    {
        super(id, source);
        this.applications.addAll(source.applications);
    }

    @Override
    protected DomainFileBuilder getThis()
    {
        return this;
    }

    /**
     * Sets the configuration file used for the domain.
     *
     * @param configFile domain configuration from a external file or test resource. Non empty.
     * @return the same builder instance
     */
    public DomainFileBuilder definedBy(String configFile)
    {
        checkImmutable();
        checkArgument(!StringUtils.isEmpty(configFile), "Config file cannot be empty");
        this.resources.add(new ZipResource(configFile, DOMAIN_CONFIG_FILE_LOCATION));

        return this;
    }

    /**
     * Adds an application to the domain.
     *
     * @param appFileBuilder builder defining the application. Non null.
     * @return the same builder instance
     */
    public DomainFileBuilder containing(ApplicationFileBuilder appFileBuilder)
    {
        checkImmutable();
        checkArgument(appFileBuilder != null, "Application builder cannot be null");
        this.applications.add(appFileBuilder);

        return this;
    }

    @Override
    protected void addCustomFileContent(ZipOutputStream out) throws Exception
    {
        for (ApplicationFileBuilder application : applications)
        {
            final File applicationFile = application.getArtifactFile();
            addZipResource(out, new ZipResource(applicationFile.getAbsolutePath(), "apps/" + applicationFile.getName()));
        }
    }

    @Override
    public String getConfigFile()
    {
        return DOMAIN_CONFIG_FILE_LOCATION;
    }
}
