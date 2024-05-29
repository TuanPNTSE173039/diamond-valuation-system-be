package com.letitbee.diamondvaluationsystem.service.impl;

import com.letitbee.diamondvaluationsystem.entity.Account;
import com.letitbee.diamondvaluationsystem.entity.Customer;
import com.letitbee.diamondvaluationsystem.entity.ValuationRequest;
import com.letitbee.diamondvaluationsystem.exception.ResourceNotFoundException;
import com.letitbee.diamondvaluationsystem.payload.CustomerDTO;
import com.letitbee.diamondvaluationsystem.payload.Response;
import com.letitbee.diamondvaluationsystem.payload.ValuationRequestDTO;
import com.letitbee.diamondvaluationsystem.repository.AccountRepository;
import com.letitbee.diamondvaluationsystem.repository.CustomerRepository;
import com.letitbee.diamondvaluationsystem.repository.ValuationRequestRepository;
import com.letitbee.diamondvaluationsystem.service.CustomerService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private CustomerRepository customerRepository;
    private ValuationRequestRepository valuationRequestRepository;
    private ModelMapper mapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, ValuationRequestRepository valuationRequestRepository, ModelMapper mapper) {
        this.customerRepository = customerRepository;
        this.valuationRequestRepository = valuationRequestRepository;
        this.mapper = mapper;
    }

    @Override
    public Response<CustomerDTO> getAllCustomer(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy) : Sort.by(sortBy).descending();
        //Set size page and pageNo
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Customer> page = customerRepository.findAll(pageable);

        List<Customer> customerList = page.getContent();

        List<CustomerDTO> customerDTOList = customerList.stream().
                map(customer -> mapToDTO(customer)).
                toList();

        Response<CustomerDTO> response = new Response<>();

        response.setContent(customerDTOList);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalPage(page.getTotalPages());
        response.setTotalElement(page.getTotalElements());
        response.setLast(page.isLast());

        return response;
    }

    @Override
    public CustomerDTO getCustomerById(long id) {
        Customer customer = customerRepository.
                findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return mapToDTO(customer);
    }

    @Override
    public CustomerDTO createCustomerInformation(CustomerDTO customerDto) {
        Account account = mapper.map(customerDto.getAccount(), Account.class);
        Customer customer = new Customer();

        customer.setAccount(account);
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setPhone(customerDto.getPhone());
        customer.setEmail(customerDto.getEmail());
        customer.setAddress(customerDto.getAddress());
        customer.setAvatar(customerDto.getAvatar());
        customer.setIdentityDocument(customerDto.getIdentityDocument());
        customer = customerRepository.save(customer);
        return mapToDTO(customer);
    }

    @Override
    public CustomerDTO updateCustomerInformation(CustomerDTO customerDto, Long id) {
        Customer customer = customerRepository.
                findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Customer", "AccountId", id));

        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setPhone(customerDto.getPhone());
        customer.setEmail(customerDto.getEmail());
        customer.setAddress(customerDto.getAddress());
        customer.setAvatar(customerDto.getAvatar());
        customer.setIdentityDocument(customerDto.getIdentityDocument());

        return mapToDTO(customerRepository.save(customer));
    }


    private CustomerDTO mapToDTO(Customer customer) {
        CustomerDTO customerDTO = mapper.map(customer, CustomerDTO.class);
        //get List valuation request
        Set<ValuationRequest> valuationRequestList = valuationRequestRepository.findAllByCustomer(customer)
                .orElse(null);
        if (valuationRequestList != null) {
            //convert to DTO
            Set<ValuationRequestDTO> valuationRequestDTOList = valuationRequestList.stream()
                    .map(valuationRequest -> (mapper.map(valuationRequest, ValuationRequestDTO.class))).collect(Collectors.toSet());
            customerDTO.setValuationRequests(valuationRequestDTOList);
        }
        return customerDTO;
    }

    private Customer mapToEntity(CustomerDTO customerDTO) {
        return mapper.map(customerDTO, Customer.class);
    }


}
