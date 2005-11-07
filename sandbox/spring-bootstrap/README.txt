From: Barry Kaplan [groups1@memelet.com]
Sent: November 3, 2005 8:05 AM
To: user@mule.codehaus.org
Subject: Re: [mule-user] Mule Spring Integration

Michael Buchsbaum wrote:

>And if I go this route, do I still need the container-context tag in 
>the mule-config.xml?
>  
>
No, with this approach the ocntainer-context is automatically set.

-barry

>Thanks
>
>On 11/1/05, Andrew Perepelytsya <aperepel@gmail.com> wrote:
>  
>
>>Barry,
>>
>>I still think Mule 1.2.x branch will not die anytime soon ;) And 
>>project already committed to Mule will not switch in-between. So, pack 
>>it up and prepare to commit after a release! :)
>>
>>Andrew
>>
>>On 11/1/05, Barry Kaplan <groups1@memelet.com> wrote:
>>    
>>
>>> I went the other way. Instead I bootstrap mule from within spring. 
>>> eg,
>>>
>>> <beans>
>>>     <bean id="muleManager" class="eg.mule.MuleManagerBean"
>>>         depends-on="jms.broker">
>>>        <property name="configResources" 
>>>value="classpath*:META-INF/services/*.mule.xml"/>
>>>     </bean>
>>>     ....
>>> </beans>
>>>
>>> The impl of MuleManagerBean will load a regular mule config, 
>>>initializing it with the enclosing spring context (ie, the context in 
>>>which the bean 'muleManager' is created).
>>>
>>> There are several other classes required to make this work:
>>>
>>>
>>>MultiModelMuleClasspathConfigurationBuilder extends 
>>>org.mule.config.builders.MuleClasspathConfigurationBuilder
>>>  - This class is not strictly needed. What it is doing is overring 
>>>the digester rule that creates mule models so that the same model is 
>>>always used. This allows for multiple mule complete mule config files 
>>>to be loaded. In the xml snippet above 
>>>"classpath*:META-INF/services/*.mule.xml" load all mule config files 
>>>with the given suffix. If you don't need this capability, the 
>>>standard mule superclass can be used.
>>>
>>>
>>>SpringContainerContext extends 
>>>org.mule.impl.container.AbstractContainerContext - This is a trivial 
>>>implementation which simply delegates the a spring context provided 
>>>in its constructor.  If you think this approach is applicable, I can 
>>>package up these few classes and put them somewhere so they can be 
>>>reused. I hestitate to commit into mule CVS since this is just a 
>>>short term solution until 2.0 is available. Mule 2.0 will no longer 
>>>use singletons and will be configurable via xbean, making all this 
>>>obsolete. But hey, I needed it now, and this is really simple.
>>>
>>> -barry
>>>
>>>
>>>
>>> Michael Buchsbaum wrote:
>>> Has anyone figured out how to configure the mule container-context 
>>>to load an existing spring context? or a parent context? Currently, 
>>>it loads a new Spring Container. I can work around it if necessary, 
>>>but it really shouldn't have to do that.
>>>
>>>
>>>
>>>
>>>--
>>> barry kaplan
>>> groups1@memelet.com
>>>      
>>>


-- 
barry kaplan
groups1@memelet.com
