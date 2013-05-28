
package org.mule.api.oauth.exception;


/**
 * Exception thrown when the request token needed for building the authorization URL
 * cannot be acquired
 */
public class UnableToAcquireRequestTokenException extends Exception
{

    private static final long serialVersionUID = 7270023278136600114L;

    public UnableToAcquireRequestTokenException(Throwable throwable)
    {
        super(throwable);
    }

}
