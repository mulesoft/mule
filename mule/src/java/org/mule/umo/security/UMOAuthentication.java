//COPYRIGHT
package org.mule.umo.security;

//AUTHOR

public interface UMOAuthentication
{
    void setAuthenticated(boolean b);

    boolean isAuthenticated();

    Object getCredentials();

    Object getDetails();

    Object getPrincipal();
}
