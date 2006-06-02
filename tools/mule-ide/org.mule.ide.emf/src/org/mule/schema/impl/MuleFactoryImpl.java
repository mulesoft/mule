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
public class MuleFactoryImpl extends EFactoryImpl implements MuleFactory {
	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case MulePackage.AGENTS_TYPE: return createAgentsType();
			case MulePackage.AGENT_TYPE: return createAgentType();
			case MulePackage.CATCH_ALL_STRATEGY_TYPE: return createCatchAllStrategyType();
			case MulePackage.COMPONENT_FACTORY_TYPE: return createComponentFactoryType();
			case MulePackage.COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE: return createComponentLifecycleAdapterFactoryType();
			case MulePackage.COMPONENT_POOL_FACTORY_TYPE: return createComponentPoolFactoryType();
			case MulePackage.CONNECTION_STRATEGY_TYPE: return createConnectionStrategyType();
			case MulePackage.CONNECTOR_TYPE: return createConnectorType();
			case MulePackage.CONSTRAINT_TYPE: return createConstraintType();
			case MulePackage.CONTAINER_CONTEXT_TYPE: return createContainerContextType();
			case MulePackage.CONTAINER_ENTRY_TYPE: return createContainerEntryType();
			case MulePackage.CONTAINER_PROPERTY_TYPE: return createContainerPropertyType();
			case MulePackage.DOCUMENT_ROOT: return createDocumentRoot();
			case MulePackage.ENCRYPTION_STRATEGY_TYPE: return createEncryptionStrategyType();
			case MulePackage.ENDPOINT_IDENTIFIERS_TYPE: return createEndpointIdentifiersType();
			case MulePackage.ENDPOINT_IDENTIFIER_TYPE: return createEndpointIdentifierType();
			case MulePackage.ENDPOINT_TYPE: return createEndpointType();
			case MulePackage.ENTRY_POINT_RESOLVER_TYPE: return createEntryPointResolverType();
			case MulePackage.ENTRY_TYPE: return createEntryType();
			case MulePackage.ENVIRONMENT_PROPERTIES_TYPE: return createEnvironmentPropertiesType();
			case MulePackage.EXCEPTION_STRATEGY_TYPE: return createExceptionStrategyType();
			case MulePackage.FACTORY_ENTRY_TYPE: return createFactoryEntryType();
			case MulePackage.FACTORY_PROPERTY_TYPE: return createFactoryPropertyType();
			case MulePackage.FILE_PROPERTIES_TYPE: return createFilePropertiesType();
			case MulePackage.FILTER_TYPE: return createFilterType();
			case MulePackage.GLOBAL_ENDPOINTS_TYPE: return createGlobalEndpointsType();
			case MulePackage.GLOBAL_ENDPOINT_TYPE: return createGlobalEndpointType();
			case MulePackage.INBOUND_ROUTER_TYPE: return createInboundRouterType();
			case MulePackage.INTERCEPTOR_STACK_TYPE: return createInterceptorStackType();
			case MulePackage.INTERCEPTOR_TYPE: return createInterceptorType();
			case MulePackage.LEFT_FILTER_TYPE: return createLeftFilterType();
			case MulePackage.LIST_TYPE: return createListType();
			case MulePackage.MAP_TYPE: return createMapType();
			case MulePackage.MODEL_TYPE: return createModelType();
			case MulePackage.MULE_CONFIGURATION_TYPE: return createMuleConfigurationType();
			case MulePackage.MULE_DESCRIPTOR_TYPE: return createMuleDescriptorType();
			case MulePackage.MULE_ENVIRONMENT_PROPERTIES_TYPE: return createMuleEnvironmentPropertiesType();
			case MulePackage.OUTBOUND_ROUTER_TYPE: return createOutboundRouterType();
			case MulePackage.PERSISTENCE_STRATEGY_TYPE: return createPersistenceStrategyType();
			case MulePackage.POOLING_PROFILE_TYPE: return createPoolingProfileType();
			case MulePackage.PROPERTIES_TYPE: return createPropertiesType();
			case MulePackage.PROPERTY_TYPE: return createPropertyType();
			case MulePackage.QUEUE_PROFILE_TYPE: return createQueueProfileType();
			case MulePackage.REPLY_TO_TYPE: return createReplyToType();
			case MulePackage.RESPONSE_ROUTER_TYPE: return createResponseRouterType();
			case MulePackage.RIGHT_FILTER_TYPE: return createRightFilterType();
			case MulePackage.ROUTER_TYPE: return createRouterType();
			case MulePackage.SECURITY_FILTER_TYPE: return createSecurityFilterType();
			case MulePackage.SECURITY_MANAGER_TYPE: return createSecurityManagerType();
			case MulePackage.SECURITY_PROVIDER_TYPE: return createSecurityProviderType();
			case MulePackage.SYSTEM_ENTRY_TYPE: return createSystemEntryType();
			case MulePackage.SYSTEM_PROPERTY_TYPE: return createSystemPropertyType();
			case MulePackage.TEXT_PROPERTY_TYPE: return createTextPropertyType();
			case MulePackage.THREADING_PROFILE_TYPE: return createThreadingProfileType();
			case MulePackage.TRANSACTION_MANAGER_TYPE: return createTransactionManagerType();
			case MulePackage.TRANSACTION_TYPE: return createTransactionType();
			case MulePackage.TRANSFORMERS_TYPE: return createTransformersType();
			case MulePackage.TRANSFORMER_TYPE: return createTransformerType();
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
			case MulePackage.ACTION_TYPE: {
				ActionType result = ActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.CREATE_CONNECTOR_TYPE: {
				CreateConnectorType result = CreateConnectorType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.ENABLE_CORRELATION_TYPE: {
				EnableCorrelationType result = EnableCorrelationType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.EXHAUSTED_ACTION_TYPE: {
				ExhaustedActionType result = ExhaustedActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.ID_TYPE: {
				IdType result = IdType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.INITIALISATION_POLICY_TYPE: {
				InitialisationPolicyType result = InitialisationPolicyType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.INITIAL_STATE_TYPE: {
				InitialStateType result = InitialStateType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE: {
				PoolExhaustedActionType result = PoolExhaustedActionType.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.TYPE_TYPE1: {
				TypeType1 result = TypeType1.get(initialValue);
				if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
				return result;
			}
			case MulePackage.ACTION_TYPE_OBJECT:
				return createActionTypeObjectFromString(eDataType, initialValue);
			case MulePackage.CREATE_CONNECTOR_TYPE_OBJECT:
				return createCreateConnectorTypeObjectFromString(eDataType, initialValue);
			case MulePackage.ENABLE_CORRELATION_TYPE_OBJECT:
				return createEnableCorrelationTypeObjectFromString(eDataType, initialValue);
			case MulePackage.EXHAUSTED_ACTION_TYPE_OBJECT:
				return createExhaustedActionTypeObjectFromString(eDataType, initialValue);
			case MulePackage.ID_TYPE_OBJECT:
				return createIdTypeObjectFromString(eDataType, initialValue);
			case MulePackage.INITIALISATION_POLICY_TYPE_OBJECT:
				return createInitialisationPolicyTypeObjectFromString(eDataType, initialValue);
			case MulePackage.INITIAL_STATE_TYPE_OBJECT:
				return createInitialStateTypeObjectFromString(eDataType, initialValue);
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE_OBJECT:
				return createPoolExhaustedActionTypeObjectFromString(eDataType, initialValue);
			case MulePackage.TYPE_TYPE:
				return createTypeTypeFromString(eDataType, initialValue);
			case MulePackage.TYPE_TYPE_OBJECT:
				return createTypeTypeObjectFromString(eDataType, initialValue);
			case MulePackage.VERSION_TYPE:
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
			case MulePackage.ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.CREATE_CONNECTOR_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.ENABLE_CORRELATION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.EXHAUSTED_ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.ID_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.INITIALISATION_POLICY_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.INITIAL_STATE_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.TYPE_TYPE1:
				return instanceValue == null ? null : instanceValue.toString();
			case MulePackage.ACTION_TYPE_OBJECT:
				return convertActionTypeObjectToString(eDataType, instanceValue);
			case MulePackage.CREATE_CONNECTOR_TYPE_OBJECT:
				return convertCreateConnectorTypeObjectToString(eDataType, instanceValue);
			case MulePackage.ENABLE_CORRELATION_TYPE_OBJECT:
				return convertEnableCorrelationTypeObjectToString(eDataType, instanceValue);
			case MulePackage.EXHAUSTED_ACTION_TYPE_OBJECT:
				return convertExhaustedActionTypeObjectToString(eDataType, instanceValue);
			case MulePackage.ID_TYPE_OBJECT:
				return convertIdTypeObjectToString(eDataType, instanceValue);
			case MulePackage.INITIALISATION_POLICY_TYPE_OBJECT:
				return convertInitialisationPolicyTypeObjectToString(eDataType, instanceValue);
			case MulePackage.INITIAL_STATE_TYPE_OBJECT:
				return convertInitialStateTypeObjectToString(eDataType, instanceValue);
			case MulePackage.POOL_EXHAUSTED_ACTION_TYPE_OBJECT:
				return convertPoolExhaustedActionTypeObjectToString(eDataType, instanceValue);
			case MulePackage.TYPE_TYPE:
				return convertTypeTypeToString(eDataType, instanceValue);
			case MulePackage.TYPE_TYPE_OBJECT:
				return convertTypeTypeObjectToString(eDataType, instanceValue);
			case MulePackage.VERSION_TYPE:
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
		return (ActionType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CreateConnectorType createCreateConnectorTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (CreateConnectorType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getCreateConnectorType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertCreateConnectorTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getCreateConnectorType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EnableCorrelationType createEnableCorrelationTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (EnableCorrelationType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getEnableCorrelationType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertEnableCorrelationTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getEnableCorrelationType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExhaustedActionType createExhaustedActionTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (ExhaustedActionType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getExhaustedActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertExhaustedActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getExhaustedActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public IdType createIdTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (IdType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getIdType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertIdTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getIdType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialisationPolicyType createInitialisationPolicyTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (InitialisationPolicyType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getInitialisationPolicyType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertInitialisationPolicyTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getInitialisationPolicyType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialStateType createInitialStateTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (InitialStateType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getInitialStateType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertInitialStateTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getInitialStateType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolExhaustedActionType createPoolExhaustedActionTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (PoolExhaustedActionType)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getPoolExhaustedActionType(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertPoolExhaustedActionTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getPoolExhaustedActionType(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String createTypeTypeFromString(EDataType eDataType, String initialValue) {
		return (String)XMLTypeFactory.eINSTANCE.createFromString(XMLTypePackage.eINSTANCE.getNMTOKEN(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTypeTypeToString(EDataType eDataType, Object instanceValue) {
		return XMLTypeFactory.eINSTANCE.convertToString(XMLTypePackage.eINSTANCE.getNMTOKEN(), instanceValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TypeType1 createTypeTypeObjectFromString(EDataType eDataType, String initialValue) {
		return (TypeType1)MuleFactory.eINSTANCE.createFromString(MulePackage.eINSTANCE.getTypeType1(), initialValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTypeTypeObjectToString(EDataType eDataType, Object instanceValue) {
		return MuleFactory.eINSTANCE.convertToString(MulePackage.eINSTANCE.getTypeType1(), instanceValue);
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
	public MulePackage getMulePackage() {
		return (MulePackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	public static MulePackage getPackage() {
		return MulePackage.eINSTANCE;
	}

} //MuleFactoryImpl
