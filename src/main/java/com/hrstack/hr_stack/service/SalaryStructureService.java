package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.SalaryStructure;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.SalaryStructureRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SalaryStructureService {

    private final SalaryStructureRepository salaryStructureRepository;

    public SalaryStructureService(
            SalaryStructureRepository salaryStructureRepository) {

        this.salaryStructureRepository =
                salaryStructureRepository;
    }

    public SalaryStructure createOrUpdateSalaryStructure(
            SalaryStructure salaryStructure) {

        Optional<SalaryStructure> existing =
                salaryStructureRepository.findByEmpId(
                        salaryStructure.getEmpId()
                );

        if (existing.isPresent()) {

            SalaryStructure existingStructure =
                    existing.get();

            existingStructure.setBasic(
                    salaryStructure.getBasic()
            );

            existingStructure.setHra(
                    salaryStructure.getHra()
            );

            existingStructure.setAllowances(
                    salaryStructure.getAllowances()
            );

            existingStructure.setPfApplicable(
                    salaryStructure.getPfApplicable()
            );

            return salaryStructureRepository.save(
                    existingStructure
            );
        }

        return salaryStructureRepository.save(
                salaryStructure
        );
    }

    public SalaryStructure getSalaryStructure(
            String empId) {

        return salaryStructureRepository
                .findByEmpId(empId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salary structure not found for employee: "
                                        + empId
                        )
                );
    }
}