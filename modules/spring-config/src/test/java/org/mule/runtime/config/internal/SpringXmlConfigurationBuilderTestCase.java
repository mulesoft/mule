package org.mule.runtime.config.internal;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.internal.xml.DefaultAstXmlParser;
import org.mule.runtime.ast.internal.xml.resolver.FailAfterTenErrorsResolveEntityFailStrategy;
import org.mule.runtime.ast.internal.xml.resolver.FailOnFirstErrorResolveEntityFailStrategy;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;

public class SpringXmlConfigurationBuilderTestCase {

    private FeatureFlaggingService featureFlaggingService;
    private SpringXmlConfigurationBuilder configurationBuilder;
    private MuleContext muleContext;

    @Before
    public void setUp() throws Exception {
        configurationBuilder = new SpringXmlConfigurationBuilder(new String[] {"dummy.xml"}, null, null, false, false);
        featureFlaggingService = mock(FeatureFlaggingService.class);
        configurationBuilder.setFeatureFlaggingService(featureFlaggingService);
        muleContext = mock(MuleContext.class);
        Injector injector = mock(Injector.class);
        when(muleContext.getInjector()).thenReturn(injector);
    }

    @Test
    @Issue("EE-7827")
    public void configureWithFailOnFirstError() throws ConfigurationException {
        when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(true);

        try {
            configurationBuilder.configure(muleContext);
        } catch (Exception ignored) {
        }

        verify(featureFlaggingService).isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR);
        assertThat(((DefaultAstXmlParser) configurationBuilder.getParser()).getFailStrategy(), instanceOf(FailOnFirstErrorResolveEntityFailStrategy.class));
    }

    @Test
    @Issue("EE-7827")
    public void configureWithFailAfterTenErrors() throws ConfigurationException {
        when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(false);

        try {
            configurationBuilder.configure(muleContext);
        } catch (Exception ignored) {
        }

        verify(featureFlaggingService).isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR);
        assertThat(((DefaultAstXmlParser) configurationBuilder.getParser()).getFailStrategy(), instanceOf(FailAfterTenErrorsResolveEntityFailStrategy.class));
    }
}