ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

CREATE TABLE employee_documents (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    employee_id UUID NOT NULL UNIQUE,

                                    id_proof_type VARCHAR(30),
                                    id_proof_number VARCHAR(100),
                                    id_proof_object_key VARCHAR(500),

                                    address_proof_type VARCHAR(30),
                                    address_proof_number VARCHAR(100),
                                    address_proof_object_key VARCHAR(500),

                                    CONSTRAINT fk_employee_documents_employee
                                        FOREIGN KEY (employee_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE
);