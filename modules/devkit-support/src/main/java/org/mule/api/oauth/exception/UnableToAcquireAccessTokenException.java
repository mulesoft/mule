
package org.mule.api.oauth.exception;

/**
 * Exception thrown when the access token needed for accessing a protected resource
 * cannot be acquired
 */
public class UnableToAcquireAccessTokenException extends Exception
{

    private static final long serialVersionUID = 5704379131295829722L;

    public UnableToAcquireAccessTokenException(Throwable throwable)
    {
        super(throwable);
    }

}
