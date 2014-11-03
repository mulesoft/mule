package org.mule.module.oauth2.internal;

public interface OAuthGrantTypeConfig
{

    /**
     * Provides support for the oauthContext MEL function for this configuration
     *
     * @param params function parameters without the config name parameter
     * @return the result of the function call
     */
    Object processOauthContextFunctionACall(final Object[] params);

}
