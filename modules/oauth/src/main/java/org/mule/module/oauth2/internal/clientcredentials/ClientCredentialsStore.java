package org.mule.module.oauth2.internal.clientcredentials;

import java.util.Collection;

public interface ClientCredentialsStore
{

    void storeAccessToken(String accessToken);

    void storeExpiresIn(String expiresIn);

    void storeCustomParameter(final String parameterName, final Object parameterValue);

    String getAccessToken();

    String getExpiresIn();

    Collection<String> getTokenResponseParametersNames();

    Object getTokenResponseParameters(String parameterName);
}
