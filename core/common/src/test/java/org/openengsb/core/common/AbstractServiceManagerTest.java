/**
 * Copyright 2010 OpenEngSB Division, Vienna University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.common;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openengsb.core.common.connectorsetupstore.ConnectorDomainPair;
import org.openengsb.core.common.connectorsetupstore.ConnectorSetupStore;
import org.openengsb.core.common.context.ContextHolder;
import org.openengsb.core.common.descriptor.ServiceDescriptor;
import org.openengsb.core.common.descriptor.ServiceDescriptor.Builder;
import org.openengsb.core.common.l10n.BundleStringsTest;
import org.openengsb.core.common.support.NullDomain;
import org.openengsb.core.common.support.NullDomainImpl;
import org.openengsb.core.common.validation.MultipleAttributeValidationResult;
import org.openengsb.core.common.validation.MultipleAttributeValidationResultImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class AbstractServiceManagerTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();
    private ConnectorSetupStore setupStoreMock;

    private final String[] domainRegistrationArray = new String[]{NullDomain.class.getName(), Domain.class.getName(),
        OpenEngSBService.class.getName()};

    private static class DummyServiceManager extends AbstractServiceManager<NullDomain, NullDomainImpl> {

        public DummyServiceManager(final NullDomainImpl instance) {
            super(new ServiceInstanceFactory<NullDomain, NullDomainImpl>() {

                @Override
                public void updateServiceInstance(NullDomainImpl instance, Map<String, String> attributes) {
                }

                @Override
                public ServiceDescriptor getDescriptor(Builder builder) {
                    builder.implementationType(NullDomainImpl.class);
                    builder.serviceType(NullDomain.class);
                    builder.name("abstract.name");
                    builder.description("abstract.description");
                    builder.id("DummyServiceManager");
                    return builder.build();
                }

                @Override
                public NullDomainImpl createServiceInstance(String id, Map<String, String> attributes) {
                    return instance;
                }

                @Override
                public MultipleAttributeValidationResult updateValidation(NullDomainImpl instance,
                    Map<String, String> attributes) {
                    return new MultipleAttributeValidationResultImpl(true, new HashMap<String, String>());
                }

                @Override
                public MultipleAttributeValidationResult createValidation(String id, Map<String, String> attributes) {
                    return new MultipleAttributeValidationResultImpl(true, new HashMap<String, String>());
                }
            });
        }

    }

    @Test
    public void testInterfaceGetters() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        DummyServiceManager manager = createDummyManager(bundleContextMock, new NullDomainImpl());
        assertThat(manager.getDomainInterface(), sameInstance(NullDomain.class));
        assertThat(manager.getImplementationClass(), sameInstance(NullDomainImpl.class));
    }

    @Test
    public void testGetDescriptor() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        DummyServiceManager manager = createDummyManager(bundleContextMock, new NullDomainImpl());

        ServiceDescriptor descriptor = manager.getDescriptor();

        assertThat(descriptor.getId(), is(DummyServiceManager.class.getSimpleName()));
        assertEquals(descriptor.getServiceType(), NullDomain.class);
        assertEquals(descriptor.getImplementationType(), NullDomainImpl.class);
    }

    @Test
    public void testAddNewOne() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put("location", "location, foo/location2");
        ContextHolder.get().setCurrentContextId("locationTest");
        NullDomainImpl instance = new NullDomainImpl();

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);

        Hashtable<String, String> props = createVerificationHashmap();
        props.put("location.locationTest", "[location][foo/location2]");

        Mockito.verify(bundleContextMock).registerService(
            eq(new String[]{NullDomain.class.getName(), Domain.class.getName(), OpenEngSBService.class.getName()}),
            any(NullDomain.class), eq(props));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(bundleContextMock).registerService(captor.capture(), any(NullDomain.class), eq(props));
        assertThat(domainRegistrationArray, equalTo(captor.getValue()));

        ConnectorDomainPair pair = new ConnectorDomainPair(NullDomain.class.getName(), instance.getClass().getName());
        Mockito.verify(setupStoreMock).storeConnectorSetup(pair, "test", attributes);
    }

    @Test
    public void testServiceRanking_ShouldBePassedToServiceRegistry() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        HashMap<String, String> attributes = new HashMap<String, String>();
        NullDomainImpl instance = new NullDomainImpl();

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        attributes.put(Constants.SERVICE_RANKING, "23");
        manager.update("test", attributes);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(bundleContextMock).registerService(any(String[].class), any(NullDomain.class), captor.capture());
        assertThat(captor.getValue().get(Constants.SERVICE_RANKING).toString(), is("23"));
    }

    @Test
    public void testUpdateExistingOne() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        HashMap<String, String> attributes = new HashMap<String, String>();

        NullDomainImpl instance = new NullDomainImpl();
        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        InOrder inOrder = Mockito.inOrder(setupStoreMock, setupStoreMock);
        manager.update("test", attributes);

        ConnectorDomainPair pair = new ConnectorDomainPair(NullDomain.class.getName(), instance.getClass().getName());
        inOrder.verify(setupStoreMock).storeConnectorSetup(pair, "test", attributes);

        HashMap<String, String> verificationAttributes = new HashMap<String, String>();
        verificationAttributes.put("foo", "bar");
        verificationAttributes.put("location", "location, foo/location2");
        ContextHolder.get().setCurrentContextId("locationTest");
        manager.update("test", verificationAttributes);

        Hashtable<String, String> props = createVerificationHashmap();
        verificationAttributes.remove("location");
        verificationAttributes.put("location.locationTest", "[location][foo/location2]");

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(bundleContextMock, Mockito.times(1)).registerService(captor.capture(), any(Proxy.class),
            eq(props));
        assertThat(domainRegistrationArray, equalTo(captor.getValue()));
        inOrder.verify(setupStoreMock).storeConnectorSetup(pair, "test", verificationAttributes);
    }

    @Test
    public void testDeleteService() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        HashMap<String, String> attributes = new HashMap<String, String>();
        NullDomainImpl instance = new NullDomainImpl();
        ServiceRegistration serviceRegistrationMock =
            appendServiceRegistrationMockToBundleContextMock(bundleContextMock, instance);

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);
        manager.delete("test");

        Mockito.verify(serviceRegistrationMock).unregister();
        ConnectorDomainPair pair = new ConnectorDomainPair(NullDomain.class.getName(), instance.getClass().getName());
        Mockito.verify(setupStoreMock).deleteConnectorSetup(pair, "test");
    }

    private ServiceRegistration appendServiceRegistrationMockToBundleContextMock(BundleContext bundleContextMock,
        NullDomainImpl mock) {
        ServiceRegistration serviceRegistrationMock = Mockito.mock(ServiceRegistration.class);
        Hashtable<String, String> props = createVerificationHashmap();
        Mockito.when(bundleContextMock.registerService(eq(domainRegistrationArray), any(NullDomain.class), eq(props)))
            .thenReturn(serviceRegistrationMock);
        return serviceRegistrationMock;
    }

    private Hashtable<String, String> createVerificationHashmap() {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("id", "test");
        props.put("domain", NullDomain.class.getName());
        props.put("class", NullDomainImpl.class.getName());
        props.put("managerId", DummyServiceManager.class.getSimpleName());
        return props;
    }

    private DummyServiceManager createDummyManager(BundleContext bundleContextMock, NullDomainImpl instance) {
        DummyServiceManager manager = new DummyServiceManager(instance);
        manager.setBundleContext(bundleContextMock);
        setupStoreMock = Mockito.mock(ConnectorSetupStore.class);
        manager.setConnectorSetupStore(setupStoreMock);
        return manager;
    }

    @Test
    public void testGetAttributeValues() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("id", "test");
        attributes.put("attribute2", "atr2");

        NullDomainImpl instance = new NullDomainImpl();

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);
        Map<String, String> attributeValues = manager.getAttributeValues("test");
        assertThat(attributes.size(), is(attributeValues.size()));
        assertThat(attributeValues.get("id"), is("test"));
        assertThat(attributeValues.get("attribute2"), is("atr2"));
    }

    @Test
    public void testGetAttributeValuesAfterUpdate() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("id", "test");
        attributes.put("attribute2", "atr2");

        NullDomainImpl instance = new NullDomainImpl();

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);

        Map<String, String> attributesNew = new HashMap<String, String>();
        attributesNew.put("id", "test");
        attributesNew.put("attribute2", "newAtr2");
        manager.update("test", attributesNew);

        Map<String, String> attributeValues = manager.getAttributeValues("test");
        assertThat(attributes.size(), is(attributeValues.size()));
        assertThat(attributeValues.get("id"), is("test"));
        assertThat(attributeValues.get("attribute2"), is("newAtr2"));
    }

    @Test
    public void testCheckIfDeletedServiceDoesNotHaveAttributeValues() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("does not exist");

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("id", "test");
        attributes.put("attribute2", "atr2");

        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        NullDomainImpl instance = new NullDomainImpl();
        ServiceRegistration serviceRegistrationMock =
            appendServiceRegistrationMockToBundleContextMock(bundleContextMock, instance);

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);
        manager.delete("test");

        Mockito.verify(serviceRegistrationMock).unregister();

        Map<String, String> attributeValues = manager.getAttributeValues("test");
        assertThat(attributeValues.size(), is(0));
    }

    @Test
    public void testIfUpdateOfSingleAttributesIsPossible() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("id", "test");
        attributes.put("attribute2", "atr2");

        NullDomainImpl instance = new NullDomainImpl();

        DummyServiceManager manager = createDummyManager(bundleContextMock, instance);
        manager.update("test", attributes);

        Map<String, String> attributesNew = new HashMap<String, String>();
        attributesNew.put("attribute2", "newAtr2");
        manager.update("test", attributesNew);

        Map<String, String> attributeValues = manager.getAttributeValues("test");
        assertThat(attributeValues.size(), is(attributes.size()));
        assertThat(attributeValues.get("id"), is("test"));
        assertThat(attributeValues.get("attribute2"), is("newAtr2"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void validationfailsForCreation_checkNotAdded() {
        ServiceInstanceFactory serviceInstanceFactory = mock(ServiceInstanceFactory.class);
        MultipleAttributeValidationResult validationResult =
            new MultipleAttributeValidationResultImpl(false, new HashMap<String, String>());
        when(serviceInstanceFactory.createValidation(Mockito.anyString(), Mockito.anyMap())).thenReturn(
            validationResult);

        AbstractServiceManager<NullDomain, NullDomainImpl> serviceManager =
            new AbstractServiceManager<NullDomain, NullDomainImpl>(serviceInstanceFactory) {
            };
        serviceManager.setConnectorSetupStore(Mockito.mock(ConnectorSetupStore.class));

        MultipleAttributeValidationResult update = serviceManager.update("id", new HashMap<String, String>());

        verify(serviceInstanceFactory, never()).createServiceInstance(Mockito.anyString(), Mockito.anyMap());
        assertThat(update, sameInstance(validationResult));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void validationfailsForUpdate_checkNotUpdated() {
        BundleContext bundleContextMock = BundleStringsTest.createBundleContextMockWithBundleStrings();
        ServiceInstanceFactory serviceInstanceFactory = mock(ServiceInstanceFactory.class);
        ServiceDescriptor descriptor = mock(ServiceDescriptor.class);
        when(descriptor.getId()).thenReturn("Something");

        when(serviceInstanceFactory.getDescriptor(Mockito.any(Builder.class))).thenReturn(descriptor);
        HashMap<String, String> attributes = new HashMap<String, String>();
        MultipleAttributeValidationResult validationResult =
            new MultipleAttributeValidationResultImpl(false, attributes);
        when(serviceInstanceFactory.createValidation(Mockito.anyString(), Mockito.anyMap())).thenReturn(
            new MultipleAttributeValidationResultImpl(true, attributes));
        when(serviceInstanceFactory.updateValidation(Mockito.any(Domain.class), Mockito.anyMap())).thenReturn(
            validationResult);
        when(serviceInstanceFactory.createServiceInstance(anyString(), anyMap())).thenReturn(new NullDomainImpl());

        AbstractServiceManager<NullDomain, NullDomainImpl> serviceManager =
            new AbstractServiceManager<NullDomain, NullDomainImpl>(serviceInstanceFactory) {
            };
        serviceManager.setConnectorSetupStore(Mockito.mock(ConnectorSetupStore.class));

        serviceManager.setBundleContext(bundleContextMock);

        String id = "id";
        serviceManager.update(id, attributes);
        MultipleAttributeValidationResult update = serviceManager.update(id, attributes);

        verify(serviceInstanceFactory).createServiceInstance(id, attributes);
        verify(serviceInstanceFactory, never()).updateServiceInstance(Mockito.any(Domain.class), Mockito.anyMap());
        assertThat(update, sameInstance(validationResult));
    }
}
