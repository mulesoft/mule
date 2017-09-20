/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunnerDelegateTo(MockitoJUnitRunner.class)
public class PetstoreSecurityContextTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String MOCK_PROVIDER = "MockProvider";
  private static final String NULL_PROVIDER = "NullProvider";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private SecurityProvider provider;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authenticationResult;

  @Override
  protected String getConfigFile() {
    return "petstore-security-context-handler.xml";
  }

  @Before
  public void setup() throws Exception {
    when(provider.getName()).thenReturn(MOCK_PROVIDER);
    when(provider.authenticate(any(Authentication.class))).thenReturn(authenticationResult);
    when(provider.createSecurityContext(authenticationResult)).thenReturn(securityContext);
    when(provider.supports(any())).thenReturn(true);

    SecurityProvider nullProvider = mock(SecurityProvider.class);
    when(nullProvider.getName()).thenReturn(NULL_PROVIDER);
    when(nullProvider.authenticate(any())).thenReturn(null);
    when(nullProvider.createSecurityContext(any())).thenReturn(null);
    when(nullProvider.supports(any())).thenReturn(false);

    muleContext.getSecurityManager().addProvider(provider);
    muleContext.getSecurityManager().addProvider(nullProvider);
  }

  @Test
  public void changedSecurityContext() throws Exception {
    SecurityContext context = flowRunner("setSecureCage").run().getSecurityContext();
    assertThat(context, is(notNullValue()));
    assertThat(context, is(securityContext));
  }

  @Test
  public void filteredProviders() throws Exception {
    SecurityContext context = flowRunner("setSecureCageFilterProviders")
        .withVariable("providers", asList(MOCK_PROVIDER))
        .run()
        .getSecurityContext();
    assertThat(context, is(notNullValue()));
    assertThat(context, is(securityContext));
  }

  @Test
  public void filteredProvidersExpectedException() throws Exception {
    expectedException.expectCause(instanceOf(SecurityProviderNotFoundException.class));
    SecurityContext context = flowRunner("setSecureCageFilterProviders")
        .withVariable("providers", asList("Invalid"))
        .run()
        .getSecurityContext();
    assertThat(context, is(notNullValue()));
    assertThat(context, is(securityContext));
  }

}
