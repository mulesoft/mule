package org.mule.runtime.config.internal;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;

import static org.mockito.Mockito.*;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;

public class SpringXmlConfigurationBuilderTestCase {

  @Test
  @Issue("EE-7827")
  public void doConfigure() throws ConfigurationException {
    SpringXmlConfigurationBuilder configurationBuilder =
        new SpringXmlConfigurationBuilder(new String[] {"dummy.xml"}, null, null, false, false);
    FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);
    Mockito.when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(true);
    configurationBuilder.setFeatureFlaggingService(featureFlaggingService);
    MuleContext muleContext = mock(MuleContext.class);
    Injector injector = mock(Injector.class);
    when(muleContext.getInjector()).thenReturn(injector);

    try {
      configurationBuilder.configure(muleContext);
    } catch (Exception ignored) {
    }

    verify(featureFlaggingService).isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR);
  }
}
