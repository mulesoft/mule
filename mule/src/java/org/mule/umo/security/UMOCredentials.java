//COPYRIGHT
package org.mule.umo.security;

//AUTHOR

public interface UMOCredentials
{
    public String getUsername();

    public char[] getPassword();

    public Object getRoles();
}
