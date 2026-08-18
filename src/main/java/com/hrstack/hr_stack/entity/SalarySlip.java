package com.hrstack.hr_stack.entity;


import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_slip",
uniqueConstraints = {
        @UniqueConstraint(
                columnNames = {"emp_id","month","year"}
        )
})
public class SalarySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id",nullable = false)
    private String empId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "working_days",nullable = false)
    private Integer workingDays;

    @Column(name = "present_days",nullable = false)
    private Integer presentDays;

    @Column(name = "absent_days",nullable = false)
    private Integer absentDays;

    @Column(name = "gross_salary",nullable = false,precision = 12,scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "deductions",nullable = false,precision = 12,scale = 2)
    private BigDecimal deduction;

    @Column(name = "net_salary",nullable = false,precision = 12,scale = 2)
    private BigDecimal netSalary;

    @Column(name = "pdf_data", columnDefinition ="BYTEA")
    private byte[] pdfData;

    public SalarySlip() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(Integer workingDays) {
        this.workingDays = workingDays;
    }

    public Integer getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(Integer presentDays) {
        this.presentDays = presentDays;
    }

    public Integer getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(Integer absentDays) {
        this.absentDays = absentDays;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }

    public BigDecimal getDeduction() {
        return deduction;
    }

    public void setDeduction(BigDecimal deduction) {
        this.deduction = deduction;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netSalary = netSalary;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
    }

}
