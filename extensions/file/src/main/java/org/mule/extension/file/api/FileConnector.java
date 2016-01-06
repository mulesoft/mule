/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static java.lang.String.format;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.module.extension.file.api.FileConnectorConfig;
import org.mule.module.extension.file.api.StandardFileSystemOperations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

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
@Operations(StandardFileSystemOperations.class)
@Providers(LocalFileConnectionProvider.class)
public class FileConnector implements Initialisable, FileConnectorConfig
{

    @Inject
    private MuleContext muleContext;

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
