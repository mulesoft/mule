/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.validation;

import static java.util.Collections.emptySet;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * An {@link AbstractConfigurationFailuresTestCase} which is expected to point to a somewhat invalid config. The test fails if the
 * config is parsed correctly.
 *
 * @since 4.0
 */
public class InvalidExtensionConfigNewTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void heisenbergMissingTls() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/heisenberg-missing-tls-connection-config.xml:17]: Element <heisenberg:secure-connection> is missing required parameter 'tlsContext'.");
    loadConfiguration("validation/heisenberg-missing-tls-connection-config.xml");
  }

  @Test
  public void petStoreMissingRequiredParameterInsidePojo() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-missing-required-parameter.xml:17]: Element <petstore:phone-number> is missing required parameter 'areaCodes'.");
    loadConfiguration("validation/petstore-missing-required-parameter.xml");
  }

  @Test
  public void operationWithExpressionConfigReference() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/operation-with-expression-config-ref.xml:19]: Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/operation-with-expression-config-ref.xml");
  }

  @Test
  public void sourceWithExpressionConfigReference() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/source-with-expression-config-ref.xml:20]: Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/source-with-expression-config-ref.xml");
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel petStore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel heisenberg = loadExtension(HeisenbergExtension.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(petStore);
    extensions.add(heisenberg);

    return extensions;
  }
}
