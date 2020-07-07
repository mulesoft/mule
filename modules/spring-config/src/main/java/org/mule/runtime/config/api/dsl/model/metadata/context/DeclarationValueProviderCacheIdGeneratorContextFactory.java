/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.context;

import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;

import java.util.Optional;

public class DeclarationValueProviderCacheIdGeneratorContextFactory {

  private static final LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>>> NULL_LAZY_VALUE =
      new LazyValue<>(Optional::empty);

  private final DslElementModelFactory dslElementModelFactory;

  public DeclarationValueProviderCacheIdGeneratorContextFactory(DslElementModelFactory dslElementModelFactory) {
    this.dslElementModelFactory = dslElementModelFactory;
  }

  public DeclarationValueProviderCacheIdGeneratorContext createContext(ParameterizedElementDeclaration declaration,
                                                                       ParameterizedModel model) {


    return new DeclarationValueProviderCacheIdGeneratorContext(getIdentifierOrFail(declaration),
                                                               declaration,
                                                               model,
                                                               empty(),
                                                               NULL_LAZY_VALUE);


  }

  public DeclarationValueProviderCacheIdGeneratorContext createContext(ParameterizedElementDeclaration declaration,
                                                                       ParameterizedModel model,
                                                                       ConfigurationElementDeclaration configDeclaration,
                                                                       ConfigurationModel configModel) {
    final ValueProviderCacheIdGeneratorContext<ParameterValue> configContext =
        new DeclarationValueProviderCacheIdGeneratorContext(getIdentifierOrFail(configDeclaration),
                                                            configDeclaration,
                                                            configModel,
                                                            empty(),
                                                            NULL_LAZY_VALUE);
    final LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>>> connectionContextProvider =
        new LazyValue<>(() -> configDeclaration
            .getConnection()
            .flatMap(connDec -> configModel
                .getConnectionProviderModel(connDec.getName())
                .map(cpm -> new DeclarationValueProviderCacheIdGeneratorContext(getIdentifierOrFail(connDec),
                                                                                connDec,
                                                                                cpm,
                                                                                empty(),
                                                                                NULL_LAZY_VALUE))));
    return new DeclarationValueProviderCacheIdGeneratorContext(getIdentifierOrFail(declaration),
                                                               declaration,
                                                               model,
                                                               Optional.of(configContext),
                                                               connectionContextProvider);
  }

  private ComponentIdentifier getIdentifierOrFail(ElementDeclaration declaration) {
    return dslElementModelFactory.create(declaration)
        .map(dslElementModel -> dslElementModel.getIdentifier()
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("DslElementModel resolved does not have identifier"))))
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not create dslElementModel for the given declaration")));
  }


}
