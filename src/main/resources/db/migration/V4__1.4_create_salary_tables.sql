
CREATE TABLE salary_structure (
                                  id BIGSERIAL PRIMARY KEY,
                                  emp_id VARCHAR(100) NOT NULL,
                                  basic NUMERIC(12, 2) NOT NULL,
                                  hra NUMERIC(12, 2) NOT NULL,
                                  allowances NUMERIC(12, 2) NOT NULL,
                                  pf_applicable BOOLEAN NOT NULL DEFAULT FALSE,

                                  CONSTRAINT fk_salary_structure_employee
                                      FOREIGN KEY (emp_id)
                                          REFERENCES users(emp_id),

                                  CONSTRAINT uq_salary_structure_emp
                                      UNIQUE (emp_id)
);


CREATE TABLE salary_slip (
                             id BIGSERIAL PRIMARY KEY,
                             emp_id VARCHAR(100) NOT NULL,
                             month INTEGER NOT NULL,
                             year INTEGER NOT NULL,
                             working_days INTEGER NOT NULL,
                             present_days INTEGER NOT NULL,
                             absent_days INTEGER NOT NULL,
                             gross_salary NUMERIC(12, 2) NOT NULL,
                             deductions NUMERIC(12, 2) NOT NULL,
                             net_salary NUMERIC(12, 2) NOT NULL,
                             pdf_url BYTEA,

                             CONSTRAINT fk_salary_slip_employee
                                 FOREIGN KEY (emp_id)
                                     REFERENCES users(emp_id),

                             CONSTRAINT uq_salary_slip_employee_month_year
                                 UNIQUE (emp_id, month, year)
);