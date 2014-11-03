package org.mule.module.oauth2.internal;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.oauth2.internal.authorizationcode.TokenResponseConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TokenResponseProcessor
{

    private final TokenResponseConfiguration tokenResponseConfiguration;
    private final ExpressionManager expressionManager;
    private final boolean retrieveRefreshToken;
    private String accessToken;
    private String refreshToken;
    private String expiresIn;
    private List<NameValuePair> customResponseParameters;

    public static TokenResponseProcessor createAuthorizationCodeProcessor(final TokenResponseConfiguration tokenResponseConfiguration, final ExpressionManager expressionManager)
    {
        return new TokenResponseProcessor(tokenResponseConfiguration, expressionManager, true);
    }

    public static TokenResponseProcessor createClientCredentialsrocessor(final TokenResponseConfiguration tokenResponseConfiguration, final ExpressionManager expressionManager)
    {
        return new TokenResponseProcessor(tokenResponseConfiguration, expressionManager, false);
    }

    private TokenResponseProcessor(final TokenResponseConfiguration tokenResponseConfiguration, final ExpressionManager expressionManager, boolean retrieveRefreshToken)
    {
        this.tokenResponseConfiguration = tokenResponseConfiguration;
        this.expressionManager = expressionManager;
        this.retrieveRefreshToken = retrieveRefreshToken;
    }

    public void process(final MuleEvent muleEvent)
    {
        accessToken = expressionManager.parse(tokenResponseConfiguration.getAccessToken(), muleEvent);
        if (retrieveRefreshToken)
        {
            refreshToken = expressionManager.parse(tokenResponseConfiguration.getRefreshToken(), muleEvent);
        }
        expiresIn = expressionManager.parse(tokenResponseConfiguration.getExpiresIn(), muleEvent);

        customResponseParameters = new ArrayList<NameValuePair>(tokenResponseConfiguration.getParameterExtractors().size());
        for (ParameterExtractor parameterExtractor : tokenResponseConfiguration.getParameterExtractors())
        {
            customResponseParameters.add(new NameValuePair(parameterExtractor.getParamName(), expressionManager.evaluate(parameterExtractor.getValue(), muleEvent)));
        }
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getExpiresIn()
    {
        return expiresIn;
    }

    public List<NameValuePair> getCustomResponseParameters()
    {
        return customResponseParameters;
    }
}
