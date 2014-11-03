package org.mule.module.oauth2.internal.clientcredentials;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ClientCredentialsStoreContextMelAdapter
{

    private final ClientCredentialsStore clientCredentialsStore;
    private final TokenResponseParameterWrapperMap tokenResponseParameterWrapperMap;

    public ClientCredentialsStoreContextMelAdapter(final ClientCredentialsStore clientCredentialsStore)
    {
        this.clientCredentialsStore = clientCredentialsStore;
        this.tokenResponseParameterWrapperMap = new TokenResponseParameterWrapperMap();
    }

    public String getAccessToken()
    {
        return clientCredentialsStore.getAccessToken();
    }

    public String getExpiresIn()
    {
        return clientCredentialsStore.getExpiresIn();
    }

    public void updateAccessToken(String accessToken)
    {
        this.clientCredentialsStore.storeAccessToken(accessToken);
    }

    public Map<String, Object> getTokenResponseParameters()
    {
        return tokenResponseParameterWrapperMap;
    }

    /**
     * Adapter so we provide support for functions oauthContext('configName').tokenResponseParameter
     */
    public class TokenResponseParameterWrapperMap implements Map<String, Object>
    {

        @Override
        public int size()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(Object key)
        {
            return clientCredentialsStore.getTokenResponseParameters((String) key);
        }

        @Override
        public Object put(String key, Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ?> m)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Object> values()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<String, Object>> entrySet()
        {
            throw new UnsupportedOperationException();
        }
    }
}
