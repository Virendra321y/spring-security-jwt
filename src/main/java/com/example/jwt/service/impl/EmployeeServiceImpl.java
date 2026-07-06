package com.example.jwt.service.impl;

import com.example.jwt.dto.EmployeeDto;
import com.example.jwt.entity.Employee;
import com.example.jwt.entity.User;
import com.example.jwt.repository.EmployeeRepository;
import com.example.jwt.repository.UserRepository;
import com.example.jwt.service.EmployeeService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user;
        if (principal instanceof User) {
            user = (User) principal;
        } else {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            try {
                Long userId = Long.parseLong(name);
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            } catch (NumberFormatException e) {
                user = userRepository.findByEmail(name)
                        .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            }
        }

        Employee employee = modelMapper.map(employeeDto, Employee.class);
        employee.setUser(user);

        Employee savedEmployee = employeeRepository.save(employee);

        return modelMapper.map(savedEmployee, EmployeeDto.class);
    }

    @Override
    public List<EmployeeDto> getAllEmployees() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user;
        if (principal instanceof User) {
            user = (User) principal;
        } else {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            try {
                Long userId = Long.parseLong(name);
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            } catch (NumberFormatException e) {
                user = userRepository.findByEmail(name)
                        .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            }
        }

        List<Employee> employees = employeeRepository.findByUser(user);
        return employees.stream()
                .map(employee -> modelMapper.map(employee, EmployeeDto.class))
                .collect(Collectors.toList());
    }
}
