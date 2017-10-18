/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.meta.ExternalLibraryType.NATIVE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_FILE_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_NAME;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Alias("secure")
@ExternalLib(name = HEISENBERG_LIB_NAME,
    description = HEISENBERG_LIB_DESCRIPTION,
    nameRegexpMatcher = HEISENBERG_LIB_FILE_NAME,
    requiredClassName = HEISENBERG_LIB_CLASS_NAME,
    type = NATIVE)
public class SecureHeisenbergConnectionProvider extends HeisenbergConnectionProvider {

  @Parameter
  private TlsContextFactory tlsContextFactory;


  @Override
  public HeisenbergConnection connect() throws ConnectionException {
    HeisenbergConnection connection = super.connect();
    connection.setTlsContextFactory(tlsContextFactory);

    return connection;
  }

  public TlsContextFactory getTlsContextFactory() {
    return tlsContextFactory;
  }
}
