# HR-Stack–Employee Onboarding System

## Requirements

- Spring Boot application
- PostgreSQL database
- Employee registration API
- Auto-generated Emp ID
- Swagger
- Login using email and password
- get employee details by email

## Employee Fields

- Employee ID (Auto-generated UUID)
- First Name
- Last Name
- Email
- Mobile Number
- password(Bcrypt encoded)

## tech stack

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven
- Swagger

## API

POST `/employee/register` – Registers a new employee.
POST `/employee/login` - login using email and password
GET `/employee/email/{email} ` - find employee by email 

## Database

- **Database:** `employee_db`
- **Table:** `employee`
