/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExternalLibraryType.NATIVE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.test.heisenberg.extension.AsyncHeisenbergSource;
import org.mule.test.heisenberg.extension.DEARadioSource;
import org.mule.test.heisenberg.extension.HeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.HeisenbergErrors;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.HeisenbergRouters;
import org.mule.test.heisenberg.extension.HeisenbergScopes;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.heisenberg.extension.KillingOperations;
import org.mule.test.heisenberg.extension.MoneyLaunderingOperation;
import org.mule.test.heisenberg.extension.ReconnectableHeisenbergSource;
import org.mule.test.heisenberg.extension.SecureHeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.CarDealer;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

public class ConfigurationInjectionLifecycleTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "heisenberg-io-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {DIHeisenbergExtension.class};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().registerCustomServiceClass("lifecycleTracker", LifecycleTracker.class);
      }
    });
    super.addBuilders(builders);
  }

  @Test
  public void injectedDependencyLifecycle() throws Exception {
    DIHeisenbergExtension heisenberg =
        (DIHeisenbergExtension) getConfigurationInstanceFromRegistry("heisenberg", testEvent(), muleContext).getValue();
    LifecycleTracker tracker = heisenberg.getLifecycleTracker();

    assertThat(tracker.initialise, is(1));
    assertThat(tracker.start, is(1));
  }

  public static class LifecycleTracker implements Initialisable, Startable {

    private int initialise, start;

    @Override
    public void initialise() throws InitialisationException {
      initialise++;
    }

    @Override
    public void start() throws MuleException {
      start++;
    }
  }

  @Extension(name = HeisenbergExtension.HEISENBERG, category = SELECT)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class,
      KillingOperations.class, HeisenbergScopes.class, HeisenbergRouters.class})
  @OnException(HeisenbergConnectionExceptionEnricher.class)
  @ConnectionProviders({HeisenbergConnectionProvider.class, SecureHeisenbergConnectionProvider.class})
  @Sources({HeisenbergSource.class, DEARadioSource.class, AsyncHeisenbergSource.class, ReconnectableHeisenbergSource.class})
  @Export(classes = {HeisenbergExtension.class, HeisenbergException.class}, resources = "methRecipe.json")
  @SubTypeMapping(baseType = Weapon.class, subTypes = {Ricin.class})
  @SubTypeMapping(baseType = Investment.class, subTypes = {CarWash.class, CarDealer.class})
  @ExternalLib(name = HeisenbergExtension.HEISENBERG_LIB_NAME, description = HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION,
      nameRegexpMatcher = HeisenbergExtension.HEISENBERG_LIB_FILE_NAME,
      requiredClassName = HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME, type = NATIVE,
      coordinates = "org.mule.libs:this-is-a-lib:dll:1.0.0")
  @ErrorTypes(HeisenbergErrors.class)
  public static class DIHeisenbergExtension extends HeisenbergExtension {

    @Inject
    private LifecycleTracker lifecycleTracker;

    @Override
    public void initialise() throws InitialisationException {
      if (lifecycleTracker.initialise != 1) {
        throw new IllegalStateException("initialise was " + lifecycleTracker.initialise);
      }
      super.initialise();
    }

    public LifecycleTracker getLifecycleTracker() {
      return lifecycleTracker;
    }
  }
}
