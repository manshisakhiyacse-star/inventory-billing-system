package com.inventory.system.service;

import com.inventory.system.dto.request.CustomerRequest;
import com.inventory.system.dto.response.CustomerResponse;
import com.inventory.system.entity.Customer;
import com.inventory.system.exception.DuplicateResourceException;
import com.inventory.system.exception.ResourceNotFoundException;
import com.inventory.system.mapper.CustomerMapper;
import com.inventory.system.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditService auditService;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            customerRepository.findByEmail(request.getEmail().trim()).ifPresent(existing -> {
                throw new DuplicateResourceException("Customer", "email", request.getEmail());
            });
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        auditService.logAction("CREATE_CUSTOMER", "Customer", savedCustomer.getId(),
                "Created customer: " + savedCustomer.getName());

        return customerMapper.toResponse(savedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            customerRepository.findByEmail(request.getEmail().trim())
                    .filter(c -> !c.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("Customer", "email", request.getEmail());
                    });
        }

        customerMapper.updateEntityFromRequest(request, customer);
        Customer updated = customerRepository.save(customer);

        auditService.logAction("UPDATE_CUSTOMER", "Customer", updated.getId(),
                "Updated customer: " + updated.getName());

        return customerMapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String search, Pageable pageable) {
        Page<Customer> page;
        if (search != null && !search.trim().isEmpty()) {
            page = customerRepository.searchByKeyword(search.trim(), pageable);
        } else {
            page = customerRepository.findByActiveTrue(pageable);
        }
        return page.map(customerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setActive(false);
        Customer updated = customerRepository.save(customer);

        auditService.logAction("DEACTIVATE_CUSTOMER", "Customer", updated.getId(),
                "Deactivated customer: " + updated.getName());

        return customerMapper.toResponse(updated);
    }
}
