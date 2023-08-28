/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.meta.ExternalLibraryType.NATIVE;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_FILE_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_NAME;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.Alias;

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
