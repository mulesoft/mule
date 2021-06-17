package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.server.HttpServer;
import org.mule.runtime.http.api.server.HttpServerFactory;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.internal.DefaultAuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.internal.config.DefaultAuthorizationCodeOAuthDancerConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mule.runtime.api.config.MuleRuntimeFeature.NO_OAUTH_REDIRECT_URI;
import static org.mule.runtime.api.util.MultiMap.emptyMultiMap;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationCodeOAuthHandlerTestCase extends AbstractMuleTestCase {

  private MuleContext muleContext;

  @Mock
  FeatureFlaggingService featureFlaggingService;
  @Mock
  LazyValue<OAuthService> oauthService;
  @Mock
  LazyValue<HttpService> httpService ;

  @InjectMocks
  AuthorizationCodeOAuthHandler authorizationCodeOAuthHandler = new AuthorizationCodeOAuthHandler();

  private AuthorizationCodeConfig oAuthConfig;
  private OAuthAuthorizationCodeDancerBuilder dancerBuilder;

  @Before
  public void setUp() throws Exception {
    muleContext = mock(MuleContext.class);
    Injector injector = mock(Injector.class);
    when(muleContext.getInjector()).thenReturn(injector);

    OAuthService oAuthService = mock(OAuthService.class);
    when(oauthService.get()).thenReturn(oAuthService);

    dancerBuilder = mock(OAuthAuthorizationCodeDancerBuilder.class, new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        if (Object.class.equals(invocation.getMethod().getReturnType())) {
          return new DefaultAuthorizationCodeOAuthDancer(new DefaultAuthorizationCodeOAuthDancerConfig());
        } else {
          return dancerBuilder;
        }
      }
    });
    when(oAuthService.authorizationCodeGrantTypeDancerBuilder(any(), any(), any())).thenReturn(dancerBuilder);

    OAuthCallbackConfig oAuthCallbackConfig = mock(OAuthCallbackConfig.class);
    oAuthConfig = new AuthorizationCodeConfig("configName",
        empty(),
        emptyMultiMap(),
        emptyMultiMap(),
        emptyMap(),
        new AuthorizationCodeGrantType("url", "url", "#[s]", "reg", "#[x]", "sd"),
        oAuthCallbackConfig,
        "key", "secret", "url", "url", "scope", "id", null, null);
    when(oAuthCallbackConfig.getExternalCallbackUrl()).thenReturn(of("http://localhost:8081/callback"));

    HttpService httpServiceMock = mock(HttpService.class);
    HttpServerFactory httpServerFactory = mock(HttpServerFactory.class);
    HttpServer httpServer = mock(HttpServer.class);
    when(httpService.get()).thenReturn(httpServiceMock);
    when(httpServiceMock.getServerFactory()).thenReturn(httpServerFactory);
    when(httpServerFactory.lookup(any())).thenReturn(httpServer);
  }

  @Test
  public void createDancerWithoutAdditionalRefreshTokenParamsWhenFeatureFlagEnabled(){
    when(featureFlaggingService.isEnabled(NO_OAUTH_REDIRECT_URI)).thenReturn(true);
    authorizationCodeOAuthHandler.register(oAuthConfig);
    verify(dancerBuilder, never()).addAdditionalRefreshTokenParameters(any());
  }

  @Test
  public void createDancerWithAdditionalRefreshTokenParamsWhenFeatureFlagNotEnabled(){
    when(featureFlaggingService.isEnabled(NO_OAUTH_REDIRECT_URI)).thenReturn(false);
    authorizationCodeOAuthHandler.register(oAuthConfig);
    verify(dancerBuilder, times(1)).addAdditionalRefreshTokenParameters(any());
  }
}
