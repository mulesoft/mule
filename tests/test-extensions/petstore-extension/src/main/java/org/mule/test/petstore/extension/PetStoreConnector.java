/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.util.List;

@Extension(name = "petstore", description = "PetStore Test connector")
@Operations({PetStoreOperations.class, PetStoreOperationsWithFailures.class, PetStoreFailingOperations.class})
@ConnectionProviders({SimplePetStoreConnectionProvider.class, PooledPetStoreConnectionProvider.class,
    TransactionalPetStoreConnectionProvider.class, PooledPetStoreConnectionProviderWithFailureInvalidConnection.class,
    PooledPetStoreConnectionProviderWithValidConnection.class})
@Sources({PetStoreSource.class, FailingPetStoreSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/petstore", prefix = "petstore")
public class PetStoreConnector {

  /**
   * Indicates how many times a {@link PetStoreConnector} was started.
   */
  public static int timesStarted;

  @Parameter
  private List<String> pets;

  @Parameter
  @Optional
  private TlsContextFactory tls;

  @Parameter
  @Optional
  private PetCage cage;

  @Parameter
  @Optional
  private List<PetCage> cages;

  @ParameterGroup(name = "Cashier")
  private ExclusiveCashier cashier;

  public List<String> getPets() {
    return pets;
  }

  public TlsContextFactory getTlsContext() {
    return tls;
  }

  public PetCage getCage() {
    return cage;
  }

  public ExclusiveCashier getCashier() {
    return cashier;
  }
}
