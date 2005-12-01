/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.xml.type.XMLTypeFactory;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

import org.mule.schema.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class SchemaFactoryImpl extends EFactoryImpl implements SchemaFactory {
	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SchemaFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case SchemaPackage.AGENTS_TYPE: return createAgentsType();
			case SchemaPackage.AGENT_TYPE: return createAgentType();
			case SchemaPackage.CATCH_ALL_STRATEGY_TYPE: return createCatchAllStrategyType();
			case SchemaPackage.COMPONENT_FACTORY_TYPE: return createComponentFactoryType();
			case SchemaPackage.COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE: return createComponentLifecycleAdapterFactoryType();
			case SchemaPackage.COMPONENT_POOL_FACTORY_TYPE: return createComponentPoolFactoryType();
			case SchemaPackage.CONNECTION_STRATEGY_TYPE: return createConnectionStrategyType();
			case SchemaPackage.CONNECTOR_TYPE: return createConnectorType();
			case SchemaPackage.CONSTRAINT_TYPE: return createConstraintType();
			case SchemaPackage.CONTAINER_CONTEXT_TYPE: return createContainerContextType();
			case SchemaPackage.CONTAINER_ENTRY_TYPE: return createContainerEntryType();
			case SchemaPackage.CONTAINER_PROPERTY_TYPE: return createContainerPropertyType();
			case SchemaPackage.DOCUMENT_ROOT: return createDocumentRoot();
			case SchemaPackage.ENCRYPTION_STRATEGY_TYPE: return createEncryptionStrategyType();
			case SchemaPackage.ENDPOINT_IDENTIFIERS_TYPE: return createEndpointIdentifiersType();
			case SchemaPackage.ENDPOINT_IDENTIFIER_TYPE: return createEndpointIdentifierType();
			case SchemaPackage.ENDPOINT_TYPE: return createEndpointType();
			case SchemaPackage.ENTRY_POINT_RESOLVER_TYPE: return createEntryPointResolverType();
			case SchemaPackage.ENTRY_TYPE: return createEntryType();
			case SchemaPackage.ENVIRONMENT_PROPERTIES_TYPE: return createEnvironmentPropertiesType();
			case SchemaPackage.EXCEPTION_STRATEGY_TYPE: return createExceptionStrategyType();
			case SchemaPackage.FACTORY_ENTRY_TYPE: return createFactoryEntryType();
			case SchemaPackage.FACTORY_PROPERTY_TYPE: return createFactoryPropertyType();
			case SchemaPackage.FILE_PROPERTIES_TYPE: return createFilePropertiesType();
			case SchemaPackage.FILTER_TYPE: return createFilterType();
			case SchemaPackage.GLOBAL_ENDPOINTS_TYPE: return createGlobalEndpointsType();
			case SchemaPackage.GLOBAL_ENDPOINT_TYPE: return createGlobalEndpointType();
			case SchemaPackage.INBOUND_ROUTER_TYPE: return createInboundRouterType();
			case SchemaPackage.INTERCEPTOR_STACK_TYPE: return createInterceptorStackType();
			case SchemaPackage.INTERCEPTOR_TYPE: return createInterceptorType();
			case SchemaPackage.LEFT_FILTER_TYPE: return createLeftFilterType();
			case SchemaPackage.LIST_TYPE: return createListType();
			case SchemaPackage.MAP_TYPE: return createMapType();
			case SchemaPackage.MODEL_TYPE: return createModelType();
			case SchemaPackage.MULE_CONFIGURATION_TYPE: return createMuleConfigurationType();
			case SchemaPackage.MULE_DESCRIPTOR_TYPE: return createMuleDescriptorType();
			case SchemaPackage.MULE_ENVIRONMENT_PROPERTIES_TYPE: return createMuleEnvironmentPropertiesType();
			case SchemaPackage.OUTBOUND_ROUTER_TYPE: return createOutboundRouterType();
			case SchemaPackage.PERSISTENCE_STRATEGY_TYPE: return createPersistenceStrategyType();
			case SchemaPackage.POOLING_PROFILE_TYPE: return createPoolingProfileType();
			case SchemaPackage.PROPERTIES_TYPE: return createPropertiesType();
			case SchemaPackage.PROPERTY_TYPE: return createPropertyType();
			case SchemaPackage.QUEUE_PROFILE_TYPE: return createQueueProfileType();
			case SchemaPackage.REPLY_TO_TYPE: return createReplyToType();
			case SchemaPackage.RESPONSE_ROUTER_TYPE: return createResponseRouterType();
			case SchemaPackage.RIGHT_FILTER_TYPE: return createRightFilterType();
			case SchemaPackage.ROUTER_TYPE: return createRouterType();
			case SchemaPackage.SECURITY_FILTER_TYPE: return createSecurityFilterType();
			case SchemaPackage.SECURITY_MANAGER_TYPE: return createSecurityManagerType();
			case SchemaPackage.SECURITY_PROVIDER_TYPE: return createSecurityProviderType();
			case SchemaPackage.SYSTEM_ENTRY_TYPE: return createSystemEntryType();
			case SchemaPackage.SYSTEM_PROPERTY_TYPE: return createSystemPropertyType();
			case SchemaPackage.TEXT_PROPERTY_TYPE: return createTextPropertyType();
			case SchemaPackage.THREADING_PROFILE_TYPE: return createThreadingProfileType();
			case SchemaPackage.TRANSACTION_MANAGER_TYPE: return createTransactionManagerType();
			case SchemaPackage.TRANSACTION_TYPE: return createTransactionType();
			case SchemaPackage.TRANSFORMERS_TYPE: return createTransformersType();
			case SchemaPackage.TRANSFORMER_TYPE: return createTransformerType();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
			case SchemaPackage.ACTION_TYPE: {
				ActionType result = ActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.CREATE_CONNECTOR_TYPE: {
				CreateConnectorType result = CreateConnectorType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.ENABLE_CORRELATION_TYPE: {
				EnableCorrelationType result = EnableCorrelationType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.EXHAUSTED_ACTION_TYPE: {
				ExhaustedActionType result = ExhaustedActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.ID_TYPE: {
				IdType result = IdType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.INITIALISATION_POLICY_TYPE: {
				InitialisationPolicyType result = InitialisationPolicyType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.INITIAL_STATE_TYPE: {
				InitialStateType result = InitialStateType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.POOL_EXHAUSTED_ACTION_TYPE: {
				PoolExhaustedActionType result = PoolExhaustedActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.TYPE_TYPE: {
				TypeType result = TypeType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case SchemaPackage.ACTION_TYPE_OBJECT:
				return createActionTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.CREATE_CONNECTOR_TYPE_OBJECT:
				return createCreateConnectorTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.ENABLE_CORRELATION_TYPE_OBJECT:
				return createEnableCorrelationTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.EXHAUSTED_ACTION_TYPE_OBJECT:
				return createExhaustedActionTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.ID_TYPE_OBJECT:
				return createIdTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.INITIALISATION_POLICY_TYPE_OBJECT:
				return createInitialisationPolicyTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.INITIAL_STATE_TYPE_OBJECT:
				return createInitialStateTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.POOL_EXHAUSTED_ACTION_TYPE_OBJECT:
				return createPoolExhaustedActionTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.TYPE_TYPE_OBJECT:
				return createTypeTypeObjectFromString(eDataType, initialValue);
			case SchemaPackage.VERSION_TYPE:
				return createVersionTypeFromString(eDataType, initialValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
			case SchemaPackage.ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.CREATE_CONNECTOR_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.ENABLE_CORRELATION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.EXHAUSTED_ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.ID_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.INITIALISATION_POLICY_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.INITIAL_STATE_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.POOL_EXHAUSTED_ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.TYPE_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case SchemaPackage.ACTION_TYPE_OBJECT:
				return convertActionTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.CREATE_CONNECTOR_TYPE_OBJECT:
				return convertCreateConnectorTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.ENABLE_CORRELATION_TYPE_OBJECT:
				return convertEnableCorrelationTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.EXHAUSTED_ACTION_TYPE_OBJECT:
				return convertExhaustedActionTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.ID_TYPE_OBJECT:
				return convertIdTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.INITIALISATION_POLICY_TYPE_OBJECT:
				return convertInitialisationPolicyTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.INITIAL_STATE_TYPE_OBJECT:
				return convertInitialStateTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.POOL_EXHAUSTED_ACTION_TYPE_OBJECT:
				return convertPoolExhaustedActionTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.TYPE_TYPE_OBJECT:
				return convertTypeTypeObjectToString(eDataType, instanceValue);
			case SchemaPackage.VERSION_TYPE:
				return convertVersionTypeToString(eDataType, instanceValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AgentsType createAgentsType() {
		AgentsTypeImpl agentsType = new AgentsTypeImpl();
		return agentsType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AgentType createAgentType() {
		AgentTypeImpl agentType = new AgentTypeImpl();
		return agentType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CatchAllStrategyType createCatchAllStrategyType() {
		CatchAllStrategyTypeImpl catchAllStrategyType = new CatchAllStrategyTypeImpl();
		return catchAllStrategyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentFactoryType createComponentFactoryType() {
		ComponentFactoryTypeImpl componentFactoryType = new ComponentFactoryTypeImpl();
		return componentFactoryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentLifecycleAdapterFactoryType createComponentLifecycleAdapterFactoryType() {
		ComponentLifecycleAdapterFactoryTypeImpl componentLifecycleAdapterFactoryType = new ComponentLifecycleAdapterFactoryTypeImpl();
		return componentLifecycleAdapterFactoryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentPoolFactoryType createComponentPoolFactoryType() {
		ComponentPoolFactoryTypeImpl componentPoolFactoryType = new ComponentPoolFactoryTypeImpl();
		return componentPoolFactoryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConnectionStrategyType createConnectionStrategyType() {
		ConnectionStrategyTypeImpl connectionStrategyType = new ConnectionStrategyTypeImpl();
		return connectionStrategyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConnectorType createConnectorType() {
		ConnectorTypeImpl connectorType = new ConnectorTypeImpl();
		return connectorType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ConstraintType createConstraintType() {
		ConstraintTypeImpl constraintType = new ConstraintTypeImpl();
		return constraintType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ContainerContextType createContainerContextType() {
		ContainerContextTypeImpl containerContextType = new ContainerContextTypeImpl();
		return containerContextType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ContainerEntryType createContainerEntryType() {
		ContainerEntryTypeImpl containerEntryType = new ContainerEntryTypeImpl();
		return containerEntryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ContainerPropertyType createContainerPropertyType() {
		ContainerPropertyTypeImpl containerPropertyType = new ContainerPropertyTypeImpl();
		return containerPropertyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public DocumentRoot createDocumentRoot() {
		DocumentRootImpl documentRoot = new DocumentRootImpl();
		return documentRoot;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EncryptionStrategyType createEncryptionStrategyType() {
		EncryptionStrategyTypeImpl encryptionStrategyType = new EncryptionStrategyTypeImpl();
		return encryptionStrategyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EndpointIdentifiersType createEndpointIdentifiersType() {
		EndpointIdentifiersTypeImpl endpointIdentifiersType = new EndpointIdentifiersTypeImpl();
		return endpointIdentifiersType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EndpointIdentifierType createEndpointIdentifierType() {
		EndpointIdentifierTypeImpl endpointIdentifierType = new EndpointIdentifierTypeImpl();
		return endpointIdentifierType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EndpointType createEndpointType() {
		EndpointTypeImpl endpointType = new EndpointTypeImpl();
		return endpointType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EntryPointResolverType createEntryPointResolverType() {
		EntryPointResolverTypeImpl entryPointResolverType = new EntryPointResolverTypeImpl();
		return entryPointResolverType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EntryType createEntryType() {
		EntryTypeImpl entryType = new EntryTypeImpl();
		return entryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EnvironmentPropertiesType createEnvironmentPropertiesType() {
		EnvironmentPropertiesTypeImpl environmentPropertiesType = new EnvironmentPropertiesTypeImpl();
		return environmentPropertiesType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExceptionStrategyType createExceptionStrategyType() {
		ExceptionStrategyTypeImpl exceptionStrategyType = new ExceptionStrategyTypeImpl();
		return exceptionStrategyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FactoryEntryType createFactoryEntryType() {
		FactoryEntryTypeImpl factoryEntryType = new FactoryEntryTypeImpl();
		return factoryEntryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FactoryPropertyType createFactoryPropertyType() {
		FactoryPropertyTypeImpl factoryPropertyType = new FactoryPropertyTypeImpl();
		return factoryPropertyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilePropertiesType createFilePropertiesType() {
		FilePropertiesTypeImpl filePropertiesType = new FilePropertiesTypeImpl();
		return filePropertiesType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilterType createFilterType() {
		FilterTypeImpl filterType = new FilterTypeImpl();
		return filterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public GlobalEndpointsType createGlobalEndpointsType() {
		GlobalEndpointsTypeImpl globalEndpointsType = new GlobalEndpointsTypeImpl();
		return globalEndpointsType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public GlobalEndpointType createGlobalEndpointType() {
		GlobalEndpointTypeImpl globalEndpointType = new GlobalEndpointTypeImpl();
		return globalEndpointType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InboundRouterType createInboundRouterType() {
		InboundRouterTypeImpl inboundRouterType = new InboundRouterTypeImpl();
		return inboundRouterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InterceptorStackType createInterceptorStackType() {
		InterceptorStackTypeImpl interceptorStackType = new InterceptorStackTypeImpl();
		return interceptorStackType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InterceptorType createInterceptorType() {
		InterceptorTypeImpl interceptorType = new InterceptorTypeImpl();
		return interceptorType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public LeftFilterType createLeftFilterType() {
		LeftFilterTypeImpl leftFilterType = new LeftFilterTypeImpl();
		return leftFilterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ListType createListType() {
		ListTypeImpl listType = new ListTypeImpl();
		return listType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MapType createMapType() {
		MapTypeImpl mapType = new MapTypeImpl();
		return mapType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ModelType createModelType() {
		ModelTypeImpl modelType = new ModelTypeImpl();
		return modelType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleConfigurationType createMuleConfigurationType() {
		MuleConfigurationTypeImpl muleConfigurationType = new MuleConfigurationTypeImpl();
		return muleConfigurationType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleDescriptorType createMuleDescriptorType() {
		MuleDescriptorTypeImpl muleDescriptorType = new MuleDescriptorTypeImpl();
		return muleDescriptorType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleEnvironmentPropertiesType createMuleEnvironmentPropertiesType() {
		MuleEnvironmentPropertiesTypeImpl muleEnvironmentPropertiesType = new MuleEnvironmentPropertiesTypeImpl();
		return muleEnvironmentPropertiesType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OutboundRouterType createOutboundRouterType() {
		OutboundRouterTypeImpl outboundRouterType = new OutboundRouterTypeImpl();
		return outboundRouterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PersistenceStrategyType createPersistenceStrategyType() {
		PersistenceStrategyTypeImpl persistenceStrategyType = new PersistenceStrategyTypeImpl();
		return persistenceStrategyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolingProfileType createPoolingProfileType() {
		PoolingProfileTypeImpl poolingProfileType = new PoolingProfileTypeImpl();
		return poolingProfileType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType createPropertiesType() {
		PropertiesTypeImpl propertiesType = new PropertiesTypeImpl();
		return propertiesType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertyType createPropertyType() {
		PropertyTypeImpl propertyType = new PropertyTypeImpl();
		return propertyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public QueueProfileType createQueueProfileType() {
		QueueProfileTypeImpl queueProfileType = new QueueProfileTypeImpl();
		return queueProfileType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ReplyToType createReplyToType() {
		ReplyToTypeImpl replyToType = new ReplyToTypeImpl();
		return replyToType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ResponseRouterType createResponseRouterType() {
		ResponseRouterTypeImpl responseRouterType = new ResponseRouterTypeImpl();
		return responseRouterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RightFilterType createRightFilterType() {
		RightFilterTypeImpl rightFilterType = new RightFilterTypeImpl();
		return rightFilterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RouterType createRouterType() {
		RouterTypeImpl routerType = new RouterTypeImpl();
		return routerType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityFilterType createSecurityFilterType() {
		SecurityFilterTypeImpl securityFilterType = new SecurityFilterTypeImpl();
		return securityFilterType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityManagerType createSecurityManagerType() {
		SecurityManagerTypeImpl securityManagerType = new SecurityManagerTypeImpl();
		return securityManagerType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityProviderType createSecurityProviderType() {
		SecurityProviderTypeImpl securityProviderType = new SecurityProviderTypeImpl();
		return securityProviderType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SystemEntryType createSystemEntryType() {
		SystemEntryTypeImpl systemEntryType = new SystemEntryTypeImpl();
		return systemEntryType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SystemPropertyType createSystemPropertyType() {
		SystemPropertyTypeImpl systemPropertyType = new SystemPropertyTypeImpl();
		return systemPropertyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TextPropertyType createTextPropertyType() {
		TextPropertyTypeImpl textPropertyType = new TextPropertyTypeImpl();
		return textPropertyType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ThreadingProfileType createThreadingProfileType() {
		ThreadingProfileTypeImpl threadingProfileType = new ThreadingProfileTypeImpl();
		return threadingProfileType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionManagerType createTransactionManagerType() {
		TransactionManagerTypeImpl transactionManagerType = new TransactionManagerTypeImpl();
		return transactionManagerType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionType createTransactionType() {
		TransactionTypeImpl transactionType = new TransactionTypeImpl();
		return transactionType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransformersType createTransformersType() {
		TransformersTypeImpl transformersType = new TransformersTypeImpl();
		return transformersType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransformerType createTransformerType() {
		TransformerTypeImpl transformerType = new TransformerTypeImpl();
		return transformerType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ActionType createActionTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (ActionType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CreateConnectorType createCreateConnectorTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (CreateConnectorType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getCreateConnectorType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertCreateConnectorTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getCreateConnectorType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EnableCorrelationType createEnableCorrelationTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (EnableCorrelationType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getEnableCorrelationType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertEnableCorrelationTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getEnableCorrelationType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExhaustedActionType createExhaustedActionTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (ExhaustedActionType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getExhaustedActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertExhaustedActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getExhaustedActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public IdType createIdTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (IdType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getIdType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertIdTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getIdType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialisationPolicyType createInitialisationPolicyTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (InitialisationPolicyType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getInitialisationPolicyType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertInitialisationPolicyTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getInitialisationPolicyType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialStateType createInitialStateTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (InitialStateType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getInitialStateType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertInitialStateTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getInitialStateType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolExhaustedActionType createPoolExhaustedActionTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (PoolExhaustedActionType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getPoolExhaustedActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertPoolExhaustedActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getPoolExhaustedActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TypeType createTypeTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (TypeType)SchemaFactory.eINSTANCE.createFromString(SchemaPackage.eINSTANCE.getTypeType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTypeTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return SchemaFactory.eINSTANCE.convertToString(SchemaPackage.eINSTANCE.getTypeType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String createVersionTypeFromString(EDataType eDataType, String initialValue) {
		return (String)XMLTypeFactory.eINSTANCE.createFromString(XMLTypePackage.eINSTANCE.getNMTOKEN(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertVersionTypeToString(EDataType eDataType, Object instanceValue) {
		return XMLTypeFactory.eINSTANCE.convertToString(XMLTypePackage.eINSTANCE.getNMTOKEN(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SchemaPackage getSchemaPackage() {
		return (SchemaPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	public static SchemaPackage getPackage() {
		return SchemaPackage.eINSTANCE;
	}

} //SchemaFactoryImpl
