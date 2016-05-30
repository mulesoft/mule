/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.file.internal.DirectoryListener;
import org.mule.extension.file.internal.LocalFilePredicateBuilder;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FilePredicateBuilder;
import org.mule.runtime.module.extension.file.api.StandardFileSystemOperations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File connector used to manipulate file systems mounted on the host
 * operation system.
 * <p>
 * This class serves as both extension definition and configuration.
 * Operations are based on the standard {@link StandardFileSystemOperations}
 *
 * @since 4.0
 */
@Extension(name = "File Connector", description = "Connector to manipulate files on a locally mounted file system")
@Operations({StandardFileSystemOperations.class})
@SubTypeMapping(baseType = FilePredicateBuilder.class, subTypes = LocalFilePredicateBuilder.class)
@Providers(LocalFileConnectionProvider.class)
@Sources(DirectoryListener.class)
public class FileConnector implements Initialisable, FileConnectorConfig
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileConnector.class);

    @ConfigName
    private String configName;

    /**
     * The directory to be considered as the root of every
     * relative path used with this connector. If not provided,
     * it will default to the value of the {@code user.home}
     * system property. If that system property is not set,
     * then the connector will fail to initialise.
     */
    @Parameter
    @Optional
    private String baseDir;

    @Override
    public void initialise() throws InitialisationException
    {
        validateBaseDir();
    }

    private void validateBaseDir() throws InitialisationException
    {
        if (baseDir == null)
        {
            baseDir = System.getProperty("user.home");
            if (baseDir == null)
            {
                throw new InitialisationException(createStaticMessage("Could not obtain user's home directory. Please provide a explicit value for the baseDir parameter"), this);
            }

            LOGGER.warn("File connector '{}' does not specify the baseDir property. Defaulting to '{}'", configName, baseDir);
        }
        Path baseDirPath = Paths.get(baseDir);
        if (Files.notExists(baseDirPath))
        {
            throw new InitialisationException(createStaticMessage(format("Provided baseDir '%s' does not exists", baseDirPath.toAbsolutePath())), this);
        }
        if (!Files.isDirectory(baseDirPath))
        {
            throw new InitialisationException(createStaticMessage(format("Provided baseDir '%s' is not a directory", baseDirPath.toAbsolutePath())), this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseDir()
    {
        return baseDir;
    }
}
