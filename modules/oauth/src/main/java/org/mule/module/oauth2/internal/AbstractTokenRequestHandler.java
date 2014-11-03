package org.mule.module.oauth2.internal;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.HttpRequester;
import org.mule.module.http.request.HttpRequesterBuilder;

public class AbstractTokenRequestHandler implements MuleContextAware
{

    private MuleContext muleContext;
    private String refreshTokenWhen = OAuthConstants.DEFAULT_REFRESH_TOKEN_WHEN_EXPRESSION;

    /**
     * @param refreshTokenWhen expression to use to determine if the response from a request to the API requires a new token
     */
    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public String getRefreshTokenWhen()
    {
        return refreshTokenWhen;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    protected MuleContext getMuleContext()
    {
        return muleContext;
    }

}
