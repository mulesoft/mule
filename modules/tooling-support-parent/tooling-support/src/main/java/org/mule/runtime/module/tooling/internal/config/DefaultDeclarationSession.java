/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticService;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;

public class DefaultDeclarationSession extends AbstractArtifactAgnosticService implements DeclarationSession {

  private LazyValue<DeclarationSession> internalConfigurationService;

  DefaultDeclarationSession(ApplicationSupplier applicationSupplier) {
    super(applicationSupplier);
    this.internalConfigurationService = new LazyValue<>(() -> {
      try {
        return createInternalService(getStartedApplication());
      } catch (ApplicationStartingException e) {
        throw new MuleRuntimeException(e.getCause());
      }
    });
  }

  private DeclarationSession createInternalService(Application application) {
    final InternalDeclarationSession internalDataProviderService =
        new InternalDeclarationSession(application.getDescriptor().getArtifactDeclaration());
    return application.getRegistry()
        .lookupByType(MuleContext.class)
        .map(muleContext -> {
          try {
            return muleContext.getInjector().inject(internalDataProviderService);
          } catch (MuleException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not inject values into DeclarationSession"));
          }
        })
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find injector to create InternalDeclarationSession")));
  }

  private DeclarationSession withInternalService() {
    return this.internalConfigurationService.get();
  }

  @Override
  public ConnectionValidationResult testConnection(String configName) {
    return withInternalService().testConnection(configName);
  }

  @Override
  public ValueResult getValues(ParameterizedElementDeclaration component, String parameterName) {
    return withInternalService().getValues(component, parameterName);
  }

  @Override
  public void dispose() {
    super.dispose();
  }

}
