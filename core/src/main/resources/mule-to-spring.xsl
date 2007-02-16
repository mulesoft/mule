<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:java="http://xml.apache.org/xslt/java"
        xmlns:helper="org.mule.config.XslHelper" exclude-result-prefixes="helper java"
        version='1.0'>

    <xsl:param name="firstContext"/>

    <xsl:output method="xml" indent="yes" encoding="ISO-8859-1" standalone="yes"
                doctype-public="-//SPRING//DTD BEAN//EN"
                doctype-system="http://www.springframework.org/dtd/spring-beans.dtd"/>

    <xsl:template match="mule-configuration">
        <beans>
            <xsl:if test="$firstContext">
                <bean id="_MuleManagemenetContextFactoryBean"
                      class="org.mule.config.spring.LegacyManagementContextFactoryBean"/>
                <bean id="_muleNameProcessor" class="org.mule.config.spring.MuleObjectNameProcessor"/>
                <bean id="_managementContextProcessor" class="org.mule.config.spring.ManagementContextPostProcessor"/>
            </xsl:if>
            <xsl:apply-templates/>
        </beans>
    </xsl:template>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="attribute::*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="mule-environment-properties">
        <bean name="muleConfiguratrion" class="org.mule.config.MuleConfiguration">
            <xsl:if test="@defaultEncoding">

                <xsl:variable name="err"
                              select="helper:reportError('The @model attribute is no longer supported on the [mule-configuration] element, since Mule now supports multiple models.')"/>
            </xsl:if>

            <xsl:if test="@recoverableMode">
                <xsl:variable name="err"
                              select="helper:reportError('The @recoverableMode attribute is no longer supported on the [mule-configuration] element.')"/>
            </xsl:if>
            <xsl:if test="@clientMode">
                <xsl:variable name="err"
                              select="helper:reportError('The @clientMode attribute can no longer be set by the user on the [mule-configuration] element.')"/>
            </xsl:if>
            <xsl:if test="@embedded">
                <xsl:variable name="err"
                              select="helper:reportError('The @embedded attribute can no longer be set by the user on the [mule-configuration] element.')"/>
            </xsl:if>
            <xsl:if test="@serverUrl">
                <xsl:variable name="err"
                              select="helper:reportError('The @serverUrl attribute is no longer supported on the [mule-configuration] element. To enable the Mule Admin agent you need to configure the agent like all other agents. For mor information see http://muledocs.org/Mule+Management+Agent')"/>
            </xsl:if>
            <xsl:if test="@enableMessageEvents">
                <xsl:variable name="err"
                              select="helper:reportError('The @enableMessageEvents attribute is no longer supported on the [mule-configuration] element. To enable the Message Notification events see http://muledocs.org/Server+Notifications')"/>
            </xsl:if>
            <xsl:if test="@synchronous">
                <property name="defaultSynchronousEndpoints">
                    <value>
                        <xsl:value-of select="@synchronous"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@remoteSync">
                <property name="defaultRemoteSync">
                    <value>
                        <xsl:value-of select="@remoteSync"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@workingDirectory">
                <property name="workingDirectory">
                    <value>
                        <xsl:value-of select="@workingDirectory"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@defaultEncoding">
                <property name="defaultEncoding">
                    <value>
                        <xsl:value-of select="@encoding"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@synchronousEventTimeout">
                <property name="defaultSynchronousEventTimeout">
                    <value>
                        <xsl:value-of select="@synchronousEventTimeout"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@transactionTimeout">
                <property name="defaultTransactionTimeout">
                    <value>
                        <xsl:value-of select="@transactionTimeout"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:apply-templates select="threading-profile" mode="global"/>
            <!-- no longer supported in this context-->
            <xsl:apply-templates select="queue-profile" mode="deprecated"/>
            <!-- no longer supported in this context -->
            <xsl:apply-templates select="pooling-profile" mode="deprecated"/>
            <!-- no longer supported in this context-->
            <xsl:apply-templates select="persistence-strategy" mode="deprecated"/>
            <xsl:apply-templates select="connection-strategy"/>
        </bean>
    </xsl:template>

    <xsl:template match="environment-properties">
        <bean name="muleEnvironmentProperties" class="org.mule.impl.container.PropertiesContainerContext">
            <property name="properties">
                <map>
                    <xsl:apply-templates select="property" mode="mapProperty"/>
                    <xsl:apply-templates select="system-property" mode="mapSystemProperty"/>
                    <xsl:apply-templates select="factory-property" mode="mapFactoryProperty"/>
                </map>
            </property>
        </bean>
    </xsl:template>

    <!-- Connector Template -->
    <xsl:template match="connector">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="@name">
                    <xsl:value-of select="@name"/>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>

        <bean name="{$name}" class="{$type}">
            <xsl:apply-templates select="properties"/>
            <xsl:apply-templates select="exception-strategy"/>
            <xsl:apply-templates select="connection-strategy"/>
            <xsl:apply-templates select="threading-profile" mode="global"/>
        </bean>
    </xsl:template>

    <!-- Transaction manager Template -->
    <xsl:template match="transaction-manager">
        <bean class="{@factory}">
            <xsl:apply-templates select="properties"/>
        </bean>
    </xsl:template>

    <!-- Agent Template -->
    <xsl:template match="agents">
        <xsl:apply-templates select="agent"/>
    </xsl:template>
    <xsl:template match="agent">
        <bean name="{@name}" class="{@className}">
            <xsl:apply-templates select="properties"/>
        </bean>
    </xsl:template>

    <xsl:template match="endpoint-identifiers|endpoint-identifier">
        <xsl:variable name="err"
                      select="helper:reportError('[endpoint-identifier] elements are no longer supported in Mule 2.0.  Instead we recommend you just use standard top-level endpoint elements (In Mule 1.x [global-endpoint] elements). For more information see http://muledocs.org/Endpoints')"/>
    </xsl:template>

    <!-- Global Endpoints -->
    <xsl:template match="global-endpoints">
        <xsl:apply-templates select="endpoint"/>
    </xsl:template>

    <!--transformer Template -->
    <xsl:template match="transformers">
        <xsl:apply-templates select="transformer"/>
    </xsl:template>

    <xsl:template match="responseTransformers">
        <xsl:apply-templates select="transformer"/>
    </xsl:template>

    <xsl:template match="transformer">
        <xsl:variable name="name">
            <xsl:value-of select="@name"/>
        </xsl:variable>
        <xsl:variable name="type">
            <xsl:value-of select="@className"/>
        </xsl:variable>
        <bean name="{$name}" class="{$type}">
            <xsl:apply-templates select="@returnClass" mode="addProperties"/>
            <xsl:apply-templates select="properties"/>
        </bean>
    </xsl:template>


    <!-- Endpoint Template -->
    <xsl:template match="endpoint|global-endpoint">
        <xsl:element name="bean">
            <xsl:if test="@name">
                <xsl:attribute name="name">
                    <xsl:value-of select="@name"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">org.mule.impl.endpoint.MuleEndpoint</xsl:attribute>
            <xsl:apply-templates select="@transformers" mode="addTransformers"/>
            <xsl:apply-templates select="@responseTransformers" mode="addTransformers"/>
            <xsl:apply-templates select="@address" mode="addEndpointURI"/>
            <xsl:apply-templates select="@createConnector"/>
            <xsl:apply-templates select="@connector"/>
            <xsl:apply-templates
                    select="@*[local-name() != 'address' and local-name() != 'transformers' and local-name() != 'createConnector' and local-name() != 'responseTransformers' and local-name() != 'connector']"
                    mode="addProperties"/>
            <xsl:apply-templates select="properties" mode="asMap"/>
            <xsl:apply-templates select="transaction"/>
            <xsl:apply-templates select="filter"/>
            <xsl:apply-templates select="security-filter"/>
        </xsl:element>
    </xsl:template>


    <xsl:template match="endpoint" mode="propertyEndpoint">
        <property name="endpoint">
            <xsl:apply-templates select="."/>
        </property>
    </xsl:template>

    <xsl:template match="global-endpoint" mode="propertyEndpoint">
        <property name="endpoint">
            <xsl:apply-templates select="."/>
        </property>
    </xsl:template>

    <xsl:template match="interceptor|interceptor-stack" mode="deprecated">
        <xsl:variable name="err"
                      select="helper:reportError('[interceptor] and [interceptor-stack] elements are no longer supported in Mule 2.0.  Instead we recommend you use Spring AOP to inject additional behaviour around your services. For more information see http://muledocs.org/Interceptors')"/>
    </xsl:template>

    <xsl:template match="model">
        <xsl:variable name="type">
            <xsl:value-of select="@type"/>
        </xsl:variable>

        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="@type='inherited'">
                    <xsl:value-of select="helper:concatId(@name)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@name"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <bean name="{$name}" class="org.mule.impl.model.ModelFactory"
              factory-method="createModel">
            <constructor-arg index="0" type="java.lang.String">
                <value>
                    <xsl:value-of select="$type"/>
                </value>
            </constructor-arg>

            <xsl:apply-templates select="bean" mode="asProperty"/>
            <xsl:if test="component-lifecycle-adapter-factory">
                <property name="lifecycleAdapterFactory">
                    <xsl:apply-templates select="component-lifecycle-adapter-factory"/>
                </property>
            </xsl:if>
            <xsl:if test="exception-strategy">
                <xsl:apply-templates select="exception-strategy"/>
            </xsl:if>
            <xsl:if test="entry-point-resolver">
                <property name="entryPointResolver">
                    <xsl:apply-templates select="entry-point-resolver"/>
                </property>
            </xsl:if>
            <property name="serviceDescriptors">
                <list>
                    <xsl:apply-templates select="mule-descriptor" mode="model"/>
                </list>
            </property>
        </bean>
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="entry-point-resolver">
        <bean name="entryPointResolver" class="{@className}"/>
    </xsl:template>

    <xsl:template match="component-lifecycle-adapter-factory">
        <bean name="componentLifecycleAdapterFactory" class="{@className}"/>
    </xsl:template>

    <xsl:template match="exception-strategy">
        <property name="exceptionListener">
            <bean class="{@className}">
                <xsl:apply-templates select="properties"/>
                <property name="endpoints">
                    <list>
                        <xsl:apply-templates select="endpoint"/>
                        <xsl:apply-templates select="global-endpoint"/>
                    </list>
                </property>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="connection-strategy">
        <property name="connectionStrategy">
            <bean class="{@className}">
                <xsl:apply-templates select="properties"/>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="mule-descriptor" mode="model">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.impl.MuleDescriptor</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <bean name="{@name}" class="{$type}">
            <property name="implementation">
                <value>
                    <xsl:value-of select="@implementation"/>
                </value>
            </property>
            <property name="modelName" value="{@currentModel}"/>
            <xsl:if test="@containerManaged">
                <xsl:variable name="err"
                          select="helper:reportError('The @containerManaged attribute is no longer supported')"/>
                <property name="containerManaged">
                    <value>
                        <xsl:value-of select="@containerManaged"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@version">
                <property name="version">
                    <value>
                        <xsl:value-of select="@version"/>
                    </value>
                </property>
            </xsl:if>
            <xsl:if test="@container">
                <xsl:choose>
                    <xsl:when test="@container='descriptor'">
                        <xsl:variable name="err"
                                      select="helper:reportError('The @container attribute can no longer be set to _descriptor_. You need to use either a class name or container reference')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <property name="container">
                            <value>
                                <xsl:value-of select="@container"/>
                            </value>
                        </property>
                    </xsl:otherwise>

                </xsl:choose>
            </xsl:if>

            <xsl:if test="@inboundEndpoint">
                <xsl:variable name="err"
                              select="helper:reportError('The @inboundEndpoint attribute is no longer supported on service descriptors, Use [endpoint] elements within the [inbound-router] instead.')"/>
            </xsl:if>
            <xsl:if test="@inboundTransformer">
                <xsl:variable name="err"
                              select="helper:reportError('The @inboundtransformer attribute is no longer supported on service descriptors, Configure the transformer on the [endpoints] within the [inbound-router] instead.')"/>
            </xsl:if>

            <xsl:if test="@outboundEndpoint">
                <xsl:variable name="err"
                              select="helper:reportError('The @outboundEndpoint attribute is no longer supported on service descriptors, Use [endpoint] elements within the [outbound-router] instead.')"/>
            </xsl:if>
            <xsl:if test="@outboundTransformer">
                <xsl:variable name="err"
                              select="helper:reportError('The @outboundtransformer attribute is no longer supported on service descriptors, Configure the transformer on the [endpoint] within the [outbound-router] instead.')"/>
            </xsl:if>

            <xsl:if test="@responseTransformer">
                <xsl:variable name="err"
                              select="helper:reportError('The @responseTransformer attribute is no longer supported on service descriptors, Configure the @responseTransformer attribute on the [endpoint] elements themselves.')"/>
            </xsl:if>

            <xsl:apply-templates select="properties" mode="asMap"/>
            <xsl:apply-templates select="inbound-router"/>
            <xsl:apply-templates select="nested-router"/>
            <xsl:apply-templates select="outbound-router"/>
            <xsl:apply-templates select="response-router"/>
            <xsl:apply-templates select="interceptor"/>
            <xsl:apply-templates select="exception-strategy"/>
            <xsl:apply-templates select="threading-profile"/>
            <xsl:apply-templates select="queue-profile" mode="deprecated"/>
            <xsl:apply-templates select="pooling-profile" mode="deprecated"/>
            <xsl:apply-templates select="bean" mode="asProperty"/>
        </bean>
    </xsl:template>


    <!-- filter Template -->
    <xsl:template match="constraint|filter|left-filter|right-filter">
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="local-name()='left-filter'">leftFilter</xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="local-name()='right-filter'">rightFilter</xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="local-name()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>

        </xsl:variable>

        <property name="{$name}">
            <bean class="{@className}">
                <xsl:apply-templates select="@*" mode="addProperties"/>
                <xsl:apply-templates select="properties"/>
                <xsl:apply-templates select="left-filter"/>
                <xsl:apply-templates select="right-filter"/>
                <xsl:apply-templates select="filter"/>
            </bean>
        </property>
    </xsl:template>

    <!-- inbound router Template -->
    <xsl:template match="inbound-router">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.routing.inbound.InboundRouterCollection</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="inboundRouter">
            <bean class="{$type}">
                <property name="endpoints">
                    <list>
                        <xsl:apply-templates select="endpoint"/>
                        <xsl:apply-templates select="global-endpoint"/>
                    </list>
                </property>
                <property name="routers">
                    <list>
                        <xsl:apply-templates select="router" mode="inbound"/>
                    </list>
                </property>
                <xsl:apply-templates select="catch-all-strategy"/>
            </bean>
        </property>
    </xsl:template>

    <!-- nested router Template -->
    <xsl:template match="nested-router">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.routing.nested.NestedRouterCollection</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="nestedRouter">
            <bean class="{$type}">
                <property name="routers">
                    <list>
                        <xsl:apply-templates select="binding"/>
                    </list>
                </property>
            </bean>
        </property>
    </xsl:template>

    <!-- Response router Template -->
    <xsl:template match="response-router">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.routing.response.ResponseRouterCollection</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="responseRouter">
            <bean class="{$type}">
                <xsl:apply-templates select="@*" mode="addProperties"/>
                <property name="endpoints">
                    <list>
                        <xsl:apply-templates select="endpoint"/>
                        <xsl:apply-templates select="global-endpoint"/>
                    </list>
                </property>
                <property name="routers">
                    <list>
                        <xsl:apply-templates select="router" mode="inbound"/>
                    </list>
                </property>
                <xsl:apply-templates select="catch-all-strategy"/>
            </bean>
        </property>
    </xsl:template>


    <!-- Outbound router Template -->
    <xsl:template match="outbound-router">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.routing.outbound.OutboundRouterCollection</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="outboundRouter">
            <bean class="{$type}">
                <xsl:apply-templates select="@*" mode="addProperties"/>
                <xsl:apply-templates select="catch-all-strategy"/>
                <property name="routers">
                    <list>
                        <xsl:apply-templates select="router"/>
                    </list>
                </property>
            </bean>
        </property>
    </xsl:template>

    <!-- Catch all strategy Template -->
    <xsl:template match="catch-all-strategy">
        <property name="catchAllStrategy">
            <bean class="{@className}">
                <xsl:apply-templates select="endpoint" mode="propertyEndpoint"/>
                <xsl:apply-templates select="global-endpoint" mode="propertyEndpoint"/>
                <xsl:apply-templates select="properties"/>
            </bean>
        </property>
    </xsl:template>

    <!-- Global Endpoint Template -->
    <xsl:template match="global-endpoint">
        <bean class="org.mule.impl.endpoint.MuleEndpoint"
              factory-method="getEndpointFromUri">
            <constructor-arg index="0" type="java.lang.String">
                <value>
                    <xsl:value-of select="@name"/>
                </value>
            </constructor-arg>
            <xsl:apply-templates select="@transformers" mode="addTransformers"/>
            <xsl:apply-templates select="@responseTransformers" mode="addTransformers"/>
            <xsl:apply-templates select="@address" mode="addEndpointURI"/>
        </bean>
    </xsl:template>

    <!-- Router Template -->
    <xsl:template match="router">
        <bean class="{@className}">
            <property name="endpoints">
                <list>
                    <xsl:apply-templates select="endpoint"/>
                    <xsl:apply-templates select="global-endpoint"/>
                </list>
            </property>
            <xsl:apply-templates select="properties"/>
            <xsl:apply-templates select="filter"/>
        </bean>
    </xsl:template>

    <!-- Nested Router binding Template -->
    <xsl:template match="binding">
        <bean class="org.mule.routing.nested.NestedRouter">
            <property name="endpoint">
                <xsl:apply-templates select="endpoint"/>
                <xsl:apply-templates select="global-endpoint"/>
            </property>
            <xsl:apply-templates select="@*" mode="addProperties"/>
        </bean>
    </xsl:template>

    <xsl:template match="router" mode="inbound">
        <bean class="{@className}">
            <xsl:apply-templates select="properties"/>
            <xsl:apply-templates select="filter"/>
        </bean>
    </xsl:template>

    <!-- Transaction Template -->
    <xsl:template match="transaction">
        <property name="transactionConfig">
            <bean class="org.mule.impl.MuleTransactionConfig">
                <xsl:if test="@action">
                    <property name="actionAsString">
                        <value>
                            <xsl:value-of select="@action"/>
                        </value>
                    </property>
                </xsl:if>
                <xsl:if test="@factory">
                    <property name="factory">
                        <bean class="{@factory}"/>
                    </property>
                </xsl:if>
                <xsl:apply-templates select="constraint"/>

            </bean>
        </property>
    </xsl:template>

    <!-- Threading Profile -->
    <xsl:template match="threading-profile" mode="global">
        <xsl:variable name="propertyName">
            <xsl:choose>
                <xsl:when test="@id='dispatcher'">dispatcherThreadingProfile</xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="@id='receiver'">receiverThreadingProfile</xsl:when>
                        <xsl:otherwise>
                            <xsl:choose>
                                <xsl:when test="@id='component'">componentThreadingProfile</xsl:when>
                                <xsl:otherwise>defaultThreadingProfile</xsl:otherwise>
                            </xsl:choose>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <property name="{$propertyName}">
            <bean class="org.mule.config.ThreadingProfile">
                <xsl:apply-templates select="@*[local-name() !='id' and local-name() !='poolExhaustedAction']|*"
                                     mode="addProperties"/>
                <xsl:if test="@poolExhaustedAction">
                    <property name="poolExhaustedActionString">
                        <value>
                            <xsl:value-of select="@poolExhaustedAction"/>
                        </value>
                    </property>
                </xsl:if>
            </bean>
        </property>

    </xsl:template>

    <xsl:template match="threading-profile">
        <property name="threadingProfile">
            <bean class="org.mule.config.ThreadingProfile">
                <xsl:apply-templates select="@*[local-name() !='id' and local-name() !='poolExhaustedAction']|*"
                                     mode="addProperties"/>
                <xsl:if test="@poolExhaustedAction">
                    <property name="poolExhaustedActionString">
                        <value>
                            <xsl:value-of select="@poolExhaustedAction"/>
                        </value>
                    </property>
                </xsl:if>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="pooling-profile">
        <property name="poolingProfile">
            <bean name="pooling-profile" class="org.mule.config.PoolingProfile">
                <xsl:apply-templates
                        select="@*[local-name() !='initialisationPolicy' and local-name() !='exhaustedAction' and local-name() !='factory']|*"
                        mode="addProperties"/>
                <xsl:if test="@exhaustedActionString">
                    <property name="exhaustedAction">
                        <value>
                            <xsl:value-of select="@exhaustedAction"/>
                        </value>
                    </property>
                </xsl:if>
                <xsl:if test="@initialisationPolicy">
                    <property name="initialisationPolicyString">
                        <value>
                            <xsl:value-of select="@initialisationPolicy"/>
                        </value>
                    </property>
                </xsl:if>
                <xsl:if test="@factory">
                    <property name="poolFactory">
                        <bean class="{@factory}"/>
                    </property>
                </xsl:if>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="queue-profile">
        <property name="queueProfile">
            <bean name="queue-profile" class="org.mule.config.QueueProfile">
                <xsl:apply-templates select="@*" mode="addProperties"/>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="queue-profile" mode="deprecated">
        <xsl:variable name="err"
                      select="helper:reportError('[queue-profile] elements are no longer supported within the mule-configuration or on non-seda service objects in Mule 2.0. For more information see http://muledocs.org/Seda+Model')"/>
    </xsl:template>

    <xsl:template match="pooling-profile" mode="deprecated">
        <xsl:variable name="err"
                      select="helper:reportError('[pooling-profile] elements are no longer supported within the mule-configuration or on non-seda service objects in Mule 2.0. For more information see http://muledocs.org/Seda+Model')"/>
    </xsl:template>

    <!-- security templates -->
    <xsl:template match="security-manager">
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>org.mule.impl.security.MuleSecurityManager</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <bean class="{$type}">
            <property name="providers">
                <list>
                    <xsl:apply-templates select="security-provider"/>
                </list>
            </property>
        </bean>
    </xsl:template>
    <xsl:template match="security-provider">
        <xsl:variable name="type" select="@className"/>
        <bean class="{$type}">
            <property name="name">
                <value>
                    <xsl:value-of select="@name"/>
                </value>
            </property>
            <xsl:apply-templates select="properties"/>
        </bean>
    </xsl:template>
    <xsl:template match="security-filter">
        <property name="securityFilter">
            <bean class="{@className}">
                <xsl:apply-templates select="properties"/>
                <xsl:if test="@useProviders">
                    NOT IMPLEMENTED
                </xsl:if>
            </bean>
        </property>
    </xsl:template>

    <!-- general utilities -->
    <xsl:template name="makeBean">
        <xsl:param name="defaultType"/>
        <xsl:variable name="type">
            <xsl:choose>
                <xsl:when test="@className">
                    <xsl:value-of select="@className"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$defaultType"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <bean name="{local-name()}" class="{$type}" autowire="constructor">
            <xsl:apply-templates select="@*|*" mode="addProperties"/>
        </bean>
    </xsl:template>


    <xsl:template match="*|@*[local-name() != 'className']" mode="addProperties">
        <property name="{local-name()}">
            <value>
                <xsl:value-of select="."/>
            </value>
        </property>
    </xsl:template>

    <xsl:template match="persistence-strategy">
        <property name="persistenceStrategy">
            <bean class="{@className}">
                <xsl:apply-templates select="properties"/>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="persistence-strategy" mode="deprecated">
        <xsl:variable name="err"
                      select="helper:reportError('[persistence-strategy] elements are no longer supported within the mule-configuration in Mule 2.0. For more information see http://muledocs.org/Persistence')"/>
    </xsl:template>

    <xsl:template match="@connector">
        <xsl:variable name="val">
            <xsl:value-of select="."/>
        </xsl:variable>
        <property name="connector">
            <ref local="{$val}"/>
        </property>
    </xsl:template>

    <xsl:template match="@createConnector">
        <property name="createConnectorAsString">
            <value>
                <xsl:value-of select="."/>
            </value>
        </property>
    </xsl:template>

    <xsl:template match="@address" mode="addEndpointURI">
        <property name="endpointURI">
            <bean class="org.mule.impl.endpoint.MuleEndpointURI">
                <constructor-arg index="0" type="java.lang.String">
                    <value>
                        <xsl:value-of select="."/>
                    </value>
                </constructor-arg>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="@inboundEndpoint|@outboundEndpoint" mode="addEndpointURI">
        <property name="{local-name()}">
            <bean class="org.mule.impl.endpoint.MuleEndpoint">
                <property name="endpointURI">
                    <bean class="org.mule.impl.endpoint.MuleEndpointURI">
                        <constructor-arg index="0" type="java.lang.String">
                            <value>
                                <xsl:value-of select="."/>
                            </value>
                        </constructor-arg>
                    </bean>
                </property>
            </bean>
        </property>
    </xsl:template>


    <xsl:template match="@transformers|@responseTransformers|@inboundTransformer|@outboundTransformer"
                  mode="addTransformers">
        <xsl:variable name="propertyName">
            <xsl:choose>
                <xsl:when test="local-name() = 'transformers'">transformer</xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="local-name() = 'responseTransformers'">responseTransformer</xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="local-name()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="{$propertyName}">
            <bean class="org.mule.util.MuleObjectHelper"
                  factory-method="getTransformer">
                <constructor-arg index="0" type="java.lang.String">
                    <value>
                        <xsl:value-of select="."/>
                    </value>
                </constructor-arg>
                <constructor-arg index="1">
                    <value>
                        <xsl:value-of select="' '"/>
                    </value>
                </constructor-arg>
            </bean>
        </property>
    </xsl:template>

    <!-- Properties  templates -->
    <!-- currently factory-property is not supported
    Nor is the default value on system-property
    or the required value on container-property
    -->
    <xsl:template match="properties">
        <xsl:apply-templates select="property"/>
        <xsl:apply-templates select="container-property"/>
        <xsl:apply-templates select="system-property"/>
        <xsl:apply-templates select="factory-property"/>
        <xsl:apply-templates select="map"/>
        <xsl:apply-templates select="list"/>
        <xsl:apply-templates select="bean"/>
        <xsl:apply-templates select="spring-property"/>
    </xsl:template>

    <xsl:template match="properties" mode="asMap">
        <property name="properties">
            <map>
                <xsl:apply-templates select="property" mode="mapProperty"/>
                <xsl:apply-templates select="container-property" mode="mapContainerProperty"/>
                <xsl:apply-templates select="system-property" mode="mapSystemProperty"/>
                <xsl:apply-templates select="factory-property" mode="mapFactoryProperty"/>
                <xsl:apply-templates select="map" mode="mapMapProperty"/>
                <xsl:apply-templates select="list" mode="mapListProperty"/>
                <xsl:apply-templates select="bean" mode="asMap"/>
                <xsl:apply-templates select="spring-property" mode="asMap"/>

            </map>
        </property>
    </xsl:template>

    <xsl:template match="property">
        <xsl:if test="@name!='org.mule.useManagerProperties'">
            <property name="{@name}">
                <value>
                    <xsl:value-of select="@value"/>
                </value>
            </property>
        </xsl:if>
    </xsl:template>

    <!-- container properties -->
    <xsl:template match="container-property">
        <property name="{@name}">
            <ref local="{@reference}"/>
        </property>
    </xsl:template>

    <!-- System Properties -->
    <xsl:template match="system-property">
        <property name="{@name}">
            <value>${
                <xsl:value-of select="@value"/>
                }
            </value>
        </property>
    </xsl:template>

    <!-- Factory Properties -->
    <xsl:template match="factory-property">
        <entry key="{@name}">
            <bean class="{@factory}"/>
        </entry>
    </xsl:template>

    <!-- Map properties -->
    <xsl:template match="map">
        <property name="{@name}">
            <map>
                <xsl:apply-templates select="property" mode="mapProperty"/>
                <xsl:apply-templates select="container-property" mode="mapContainerProperty"/>
                <xsl:apply-templates select="system-property" mode="mapSystemProperty"/>
                <xsl:apply-templates select="factory-property" mode="mapFactoryProperty"/>
            </map>
        </property>
    </xsl:template>

    <xsl:template match="property" mode="mapProperty">
        <entry key="{@name}">
            <value>
                <xsl:value-of select="@value"/>
            </value>
        </entry>
    </xsl:template>

    <!-- container properties -->
    <xsl:template match="container-property" mode="mapContainerProperty">
        <entry key="{@name}">
            <ref local="{@reference}"/>
        </entry>
    </xsl:template>

    <!-- System Properties -->
    <xsl:template match="system-property" mode="mapSystemProperty">
        <entry key="{@name}">
            <value>${
                <xsl:value-of select="@key"/>
                }
            </value>
        </entry>
    </xsl:template>

    <!-- Factory Properties in a Map -->
    <xsl:template match="factory-property" mode="mapFactoryProperty">
        <entry key="{@name}">
            <bean class="{@factory}"/>
        </entry>
    </xsl:template>

    <!-- List Properties in a Map -->
    <xsl:template match="list" mode="mapListProperty">
        <entry key="{@name}">
            <list>
                <xsl:apply-templates select="entry"/>
                <xsl:apply-templates select="container-entry"/>
                <xsl:apply-templates select="system-entry"/>
                <xsl:apply-templates select="factory-entry"/>
            </list>
        </entry>
    </xsl:template>

    <!-- Map Properties in a Map -->
    <xsl:template match="map" mode="mapMapProperty">
        <entry key="{@name}">
            <map>
                <xsl:apply-templates select="property" mode="mapProperty"/>
                <xsl:apply-templates select="container-property" mode="mapContainerProperty"/>
                <xsl:apply-templates select="system-property" mode="mapSystemProperty"/>
                <xsl:apply-templates select="factory-property" mode="mapFactoryProperty"/>
            </map>
        </entry>
    </xsl:template>

    <!-- List properties -->
    <xsl:template match="list">
        <property name="{@name}">
            <list>
                <xsl:apply-templates select="entry"/>
                <xsl:apply-templates select="container-entry"/>
                <xsl:apply-templates select="system-entry"/>
                <xsl:apply-templates select="factory-entry"/>
            </list>
        </property>
    </xsl:template>

    <xsl:template match="entry">
        <value>
            <xsl:value-of select="@value"/>
        </value>
    </xsl:template>

    <xsl:template match="container-entry">
        <ref local="{@value}"/>
    </xsl:template>

    <!-- Factory Entry -->
    <xsl:template match="factory-entry">
        <bean class="{@factory}"/>
    </xsl:template>

    <xsl:template match="system-entry">
        <value>${
            <xsl:value-of select="@value"/>
            }
        </value>
    </xsl:template>

    <!--
Templates for processing interleaved bean configuration
    -->

    <xsl:template match="bean" mode="asProperty">
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="@name">
                    <xsl:value-of select="@name"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@id"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <property name="{$name}">
            <bean name="{$name}" class="{@class}">
                <xsl:apply-templates/>
            </bean>
        </property>
    </xsl:template>

    <xsl:template match="bean" mode="asMap">
        <xsl:variable name="name">
            <xsl:choose>
                <xsl:when test="@name">
                    <xsl:value-of select="@name"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@id"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <entry key="{$name}">
            <bean name="{$name}" class="{@class}">
                <xsl:apply-templates/>
            </bean>
        </entry>
    </xsl:template>


    <xsl:template match="spring-property">
        <property name="{@name}">
            <xsl:apply-templates/>
        </property>
    </xsl:template>

    <xsl:template match="spring-property" mode="asMap">
        <entry key="{@name}">
            <xsl:apply-templates/>
        </entry>
    </xsl:template>

    <xsl:template match="spring-map">
        <map>
            <xsl:apply-templates/>
        </map>
    </xsl:template>

    <xsl:template match="spring-list">
        <list>
            <xsl:apply-templates/>
        </list>
    </xsl:template>

    <xsl:template match="spring-entry">
        <entry key="{@key}">
            <xsl:apply-templates/>
        </entry>
    </xsl:template>
</xsl:stylesheet>
