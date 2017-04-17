/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration;

import org.mule.extension.db.internal.StatementStreamingResultSetCloser;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Interface to extract the common {@link ArtifactClassLoaderRunnerConfig} for all DB Test Cases without the
 * need of extend of a unique abstract test case.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    exportPluginClasses = {DbConnectionProvider.class, StatementStreamingResultSetCloser.class},
    sharedRuntimeLibs = {"org.apache.derby:derby"})
public interface DbArtifactClassLoaderRunnerConfig {
}
