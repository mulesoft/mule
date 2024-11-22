/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.api.annotation.NoImplement;

import java.nio.charset.Charset;

/**
 * Allows to query the encoding configured for the current deployable artifact.
 * 
 * @since 4.9
 */
@NoImplement
public interface ArtifactEncoding {

  /**
   * @return the configured default encoding, checking in the following order until a value is found:
   *         <ul>
   *         <li>{@code configuration} of the artifact ->
   *         {@link org.mule.runtime.core.api.config.MuleConfiguration#getDefaultEncoding()}</li>
   *         <li>The value of the system property 'mule.encoding'</li>
   *         <li>{@code Charset.defaultCharset()}</li>
   *         </ul>
   */
  Charset getDefaultEncoding();
}
