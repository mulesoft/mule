package org.mule.module.oauth2.internal;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.HttpRequester;
import org.mule.module.http.request.HttpRequesterBuilder;

public class AutoTokenRequestHandler extends AbstractTokenRequestHandler implements Initialisable
{

    private HttpRequester httpRequester;
    private String tokenUrl;

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            httpRequester = new HttpRequesterBuilder(getMuleContext())
                    .setAddress(tokenUrl)
                    .setMethod("POST")
                    .build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    protected MuleEvent invokeTokenUrl(final MuleEvent event) throws MuleException
    {
        return httpRequester.process(event);
    }
}
