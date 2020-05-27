package org.mule.tooling.extensions.metadata.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Alias("tstConnection")
public class TstConnectionProvider implements ConnectionProvider<TstExtensionClient> {

  @Parameter
  private String clientName;

  @Override
  public TstExtensionClient connect() throws ConnectionException {
    return new TstExtensionClient(clientName);
  }

  @Override
  public void disconnect(TstExtensionClient ewmClient) {}

  @Override
  public ConnectionValidationResult validate(TstExtensionClient tstClient) {
      return success();
  }
}
