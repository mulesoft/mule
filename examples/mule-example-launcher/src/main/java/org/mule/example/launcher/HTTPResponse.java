/**
 * 
 */
package org.mule.example.launcher;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author mariano
 *
 */
public class HTTPResponse
{
	@JsonProperty
	private String response;

	/**
	 * 
	 */
	public HTTPResponse()
	{
		this(null);
	}

	public HTTPResponse(String response)
	{
		super();
		setResponse(response);
	}
	
	public String getResponse()
	{
		return response;
	}

	public void setResponse(String response)
	{
		this.response = response;
	}

	
}
