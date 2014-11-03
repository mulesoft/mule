package org.mule.module.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.HttpHeaders;
import org.mule.module.oauth2.AbstractOAuthAuthorizationTestCase;
import org.mule.module.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.module.oauth2.internal.OAuthConstants;
import org.mule.module.oauth2.internal.authorizationcode.AuthorizationCodeConfig;
import org.mule.module.oauth2.internal.authorizationcode.state.UserOAuthContext;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

public class AbstractAuthorizationCodeRefreshTokenConfigTestCase extends AbstractOAuthAuthorizationTestCase
{

    private static final String RESOURCE_PATH = "/resource";
    public static final String RESOURCE_RESULT = "resource result";
    public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/redirect", localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenHost = new SystemProperty("token.host", String.format("localhost"));
    @Rule
    public SystemProperty tokenPort = new SystemProperty("token.port", String.valueOf(oauthServerPort.getNumber()));
    @Rule
    public SystemProperty tokenPath = new SystemProperty("token.path", TOKEN_PATH);

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-refresh-token-config.xml";
    }

    protected void executeRefreshToken(String flowName, String oauthConfigName, String userId, int failureStatusCode) throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(REFRESHED_ACCESS_TOKEN);

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(REFRESHED_ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withBody(RESOURCE_RESULT)));
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(failureStatusCode)
                                                         .withBody("")));

        final UserOAuthContext userOauthContext = muleContext.getRegistry().<AuthorizationCodeConfig>lookupObject(oauthConfigName).getConfigOAuthContext().getContextForUser(userId);
        userOauthContext.setAccessToken(ACCESS_TOKEN);
        userOauthContext.setRefreshToken(REFRESH_TOKEN);

        Flow flow = (Flow) getFlowConstruct(flowName);
        final MuleEvent testEvent = getTestEvent("message");
        testEvent.setFlowVariable("userId", userId);
        final MuleEvent result = flow.process(testEvent);
        assertThat(result.getMessage().getPayloadAsString(), is(RESOURCE_RESULT));

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REFRESH_TOKEN_PARAMETER + "=" + URLEncoder.encode(REFRESH_TOKEN, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_REFRESH_TOKEN, StandardCharsets.UTF_8.name()))));
    }

}

