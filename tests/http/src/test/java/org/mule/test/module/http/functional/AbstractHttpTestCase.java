/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import org.mule.extension.http.internal.request.validator.HttpRequesterConfig;
import org.mule.extension.http.internal.request.validator.HttpRequesterProvider;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(exportClasses = {HttpRequesterProvider.class, HttpRequesterConfig.class})
public abstract class AbstractHttpTestCase extends MuleArtifactFunctionalTestCase {

  protected static final int DEFAULT_TIMEOUT = 1000;
}
