package com.inventory.system.service;

import com.inventory.system.dto.request.CustomerRequest;
import com.inventory.system.dto.response.CustomerResponse;
import com.inventory.system.entity.Customer;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.CustomerMapper;
import com.inventory.system.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Should create customer successfully")
    void testCreateCustomerSuccess() {
        CustomerRequest request = CustomerRequest.builder()
                .name("Global Logistics")
                .email("contact@globallogistics.com")
                .phone("555-0199")
                .build();

        Customer customer = Customer.builder()
                .id(1L)
                .name("Global Logistics")
                .email("contact@globallogistics.com")
                .build();

        CustomerResponse responseDto = CustomerResponse.builder()
                .id(1L)
                .name("Global Logistics")
                .email("contact@globallogistics.com")
                .build();

        when(customerRepository.findByEmail("contact@globallogistics.com")).thenReturn(Optional.empty());
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(responseDto);

        CustomerResponse result = customerService.createCustomer(request);

        assertNotNull(result);
        assertEquals("Global Logistics", result.getName());
        verify(auditService).logAction(eq("CREATE_CUSTOMER"), eq("Customer"), eq(1L), anyString());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void testCreateCustomerDuplicateEmail() {
        CustomerRequest request = CustomerRequest.builder()
                .name("Client B")
                .email("existing@client.com")
                .build();

        when(customerRepository.findByEmail("existing@client.com"))
                .thenReturn(Optional.of(new Customer()));

        assertThrows(DuplicateResourceException.class, () -> customerService.createCustomer(request));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent customer")
    void testGetCustomerNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(404L));
    }
}
