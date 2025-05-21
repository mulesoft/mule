/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.param.RuntimeVersion;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Extension(name = "petstore")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Operations({PetStoreOperations.class, PetStoreOperationsWithFailures.class, PetStoreFailingOperations.class})
@ConnectionProviders({SimplePetStoreConnectionProvider.class, PooledPetStoreConnectionProvider.class,
    TransactionalPetStoreConnectionProvider.class, PooledPetStoreConnectionProviderWithFailureInvalidConnection.class,
    PooledPetStoreConnectionProviderWithValidConnection.class})
@ErrorTypes(PetstoreErrorTypeDefinition.class)
@Sources({PetStoreSource.class, FailingPetStoreSource.class, SentientSource.class, PetAdoptionSource.class,
    PetAdoptionSchedulerInParamSource.class, PetStoreStreamSource.class, PartialPetAdoptionSource.class,
    ConnectedPetAdoptionSource.class, PetFailingPollingSource.class, PetFilterPollingSource.class,
    PetStoreSimpleSourceWithSdkApi.class, PetStoreListSourceLegacyCallback.class, InvalidIgnoredSource.class})
@org.mule.sdk.api.annotation.Sources({NumberPetAdoptionSource.class, WatermarkingPetAdoptionSource.class,
    PetStoreListSource.class, PetAdoptionLimitingSource.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/petstore", prefix = "petstore")
public class PetStoreConnector {

  /**
   * Indicates how many times a {@link PetStoreConnector} was started.
   */
  private static AtomicInteger timesStarted = new AtomicInteger();
  // ref: MULE-19264. Created to cover the case of a parameter group name with spaces.
  @ParameterGroup(name = "Advanced Leash Configuration", showInDsl = true)
  public AdvancedLeashConfiguration advancedLeashConfiguration;
  @DefaultEncoding
  String encoding;
  @RuntimeVersion
  MuleVersion runtimeVersion;
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
  @Parameter
  @Optional
  private Aquarium aquarium;

  public static int getTimesStarted() {
    return timesStarted.get();
  }

  public static int incTimesStarted() {
    return timesStarted.incrementAndGet();
  }

  public static void clearTimesStarted() {
    timesStarted.set(0);
  }

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

  public String getEncoding() {
    return encoding;
  }

  public TlsContextFactory getTls() {
    return tls;
  }

  public List<PetCage> getCages() {
    return cages;
  }

  public Aquarium getAquarium() {
    return aquarium;
  }

  public MuleVersion getRuntimeVersion() {
    return runtimeVersion;
  }
}
