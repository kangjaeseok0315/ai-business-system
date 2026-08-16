package backend.customer;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;


    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @GetMapping
    public List<Customer> findAll(){
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer findOne(@PathVariable Long id){
        return customerRepository.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer request){
        Customer customer = customerRepository.findById(id).orElseThrow();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        return customerRepository.save(customer);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        customerRepository.deleteById(id);
    }


}
