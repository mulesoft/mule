/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.extensions;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

/**
 * Provides the extension dependencies of the compatibility plugin for runnint its tests in an isolated manner.
 * 
 * @since 4.0
 */
public abstract class CompatibilityFunctionalTestCase extends MuleArtifactFunctionalTestCase
    implements CompatibilityFunctionalTestCaseRunnerConfig {

}
