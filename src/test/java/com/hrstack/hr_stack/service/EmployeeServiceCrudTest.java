package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.dto.RegisterEmployeeRequest;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeDocumentRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService CRUD")
class EmployeeServiceCrudTest {

    private static final UUID EMP_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "user@example.com";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeDocumentRepository employeeDocumentRepository;
    @Mock private MinioStorageService minioStorageService;
    @Mock private InAppNotificationService inAppNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks
    private EmployeeService service;

    @BeforeEach
    void wireBuckets() {
        ReflectionTestUtils.setField(service, "tempBucket", "temp-bucket");
        ReflectionTestUtils.setField(service, "permanentBucket", "permanent-bucket");
    }

    // ---------- helpers ----------

    private static Employee employee(String role, String status) {
        Employee e = new Employee(EMP_ID, "testing", "Test", EMAIL,
                "9999999999", "Test@1234", role);
        e.setEmpId("HRStack_2025_IT_001");
        e.setStatus(status);
        e.setCreatedOn(System.currentTimeMillis());
        e.setUpdatedOn(System.currentTimeMillis());
        return e;
    }

    private static RegisterEmployeeRequest registerRequest() {
        RegisterEmployeeRequest r = new RegisterEmployeeRequest();
        r.setFirstName("testing");
        r.setLastName("Test");
        r.setEmail(EMAIL);
        r.setMobile("9999999999");
        r.setPassword("Test@1234");
        r.setTwoFactorEnabled(false);
        return r;
    }

    private static void assertBadRequest(String expectedMessage, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertEquals(expectedMessage, ex.getMessage());
    }

    private static Page<Employee> emptyPage() {
        return new PageImpl<>(List.of());
    }

    // ============================================================
    // registerEmployee
    // ============================================================
    @Nested
    @DisplayName("registerEmployee")
    class RegisterEmployee {

        @Test
        @DisplayName("duplicate email -> BadRequest, no save, no verification check")
        void duplicateEmail() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

            assertBadRequest("Email already exists. Please use another email.",
                    () -> service.registerEmployee(registerRequest()));

            verifyNoInteractions(emailVerificationService);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("email not verified -> assertVerified throws, no save")
        void notVerified() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            org.mockito.Mockito.doThrow(
                            new BadRequestException("Please verify your email before registering."))
                    .when(emailVerificationService).assertVerified(EMAIL);

            assertBadRequest("Please verify your email before registering.",
                    () -> service.registerEmployee(registerRequest()));

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("success -> hashes password, forces EMPLOYEE role, generates empId, notifies admins")
        void success() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            when(employeeRepository.findAll()).thenReturn(List.of());
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(employeeRepository.findByRoleIgnoreCase("ADMIN"))
                    .thenReturn(List.of(employee("ADMIN", "ACTIVE")));

            Employee saved = service.registerEmployee(registerRequest());

            assertEquals("EMPLOYEE", saved.getRole());
            assertTrue(saved.getPassword().startsWith("$2"));
            assertNotEquals("Test@1234", saved.getPassword());   // hashed
            assertNotNull(saved.getEmpId());
            assertTrue(saved.getEmpId().startsWith("HRStack_" + Year.now().getValue() + "_IT_"));
            assertNotNull(saved.getCreatedOn());
            assertNotNull(saved.getUpdatedOn());

            verify(emailVerificationService).assertVerified(EMAIL);
            verify(emailVerificationService).consume(EMAIL);
            verify(inAppNotificationService).createNotification(
                    any(), eq("NEW_EMPLOYEE_REGISTERED"), anyString());
        }
    }

    // ============================================================
    // createAdmin
    // ============================================================
    @Nested
    @DisplayName("createAdmin")
    class CreateAdmin {

        @Test
        @DisplayName("duplicate email -> BadRequest")
        void duplicateEmail() {
            Employee admin = new Employee();
            admin.setEmail(EMAIL);
            admin.setPassword("Test@1234");
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

            assertBadRequest("Email already exists. Please use another email.",
                    () -> service.createAdmin(admin));

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("success -> forces ADMIN role, hashes password, generates empId")
        void success() {
            Employee newAdmin = new Employee();
            newAdmin.setFirstName("Admin");
            newAdmin.setLastName("One");
            newAdmin.setEmail(EMAIL);
            newAdmin.setMobile("9999999999");
            newAdmin.setPassword("Test@1234");
            newAdmin.setRole("EMPLOYEE");   // client tried to spoof role
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            when(employeeRepository.findAll()).thenReturn(List.of());
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Employee saved = service.createAdmin(newAdmin);

            assertEquals("ADMIN", saved.getRole());   // forced
            assertTrue(saved.getPassword().startsWith("$2"));
            assertNotNull(saved.getEmpId());
        }
    }

    // ============================================================
    // getAllEmployees / getAllAdmins
    // ============================================================
    @Nested
    @DisplayName("getAllEmployees / getAllAdmins")
    class Pagination {

        @Test
        @DisplayName("getAllEmployees rejects unknown status filter")
        void invalidStatus() {
            assertBadRequest("Invalid status filter.",
                    () -> service.getAllEmployees("", "BANANA", null, null, 0, 10));

            verifyNoInteractions(employeeRepository);
        }

        @Test
        @DisplayName("getAllEmployees rejects from > to")
        void fromAfterTo() {
            assertBadRequest("Start date cannot be after end date.",
                    () -> service.getAllEmployees("", "", 200L, 100L, 0, 10));

            verifyNoInteractions(employeeRepository);
        }

        @Test
        @DisplayName("getAllEmployees clamps page size to 50 and negative page to 0")
        void clampsPage() {
            when(employeeRepository.searchByRole(
                    anyString(), anyString(), anyLong(), anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.getAllEmployees("", "", null, null, -3, 999);

            ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(employeeRepository).searchByRole(
                    eq("EMPLOYEE"), eq(""), eq(0L), eq(Long.MAX_VALUE),
                    anyString(), pageCaptor.capture());
            assertEquals(0, pageCaptor.getValue().getPageNumber());
            assertEquals(50, pageCaptor.getValue().getPageSize());
        }

        @Test
        @DisplayName("getAllEmployees truncates search > 50 chars")
        void truncatesSearch() {
            when(employeeRepository.searchByRole(
                    anyString(), anyString(), anyLong(), anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            String longSearch = "a".repeat(80);
            service.getAllEmployees(longSearch, "", null, null, 0, 10);

            ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
            verify(employeeRepository).searchByRole(
                    eq("EMPLOYEE"), eq(""), eq(0L), eq(Long.MAX_VALUE),
                    patternCaptor.capture(), any(Pageable.class));
            assertEquals("%" + "a".repeat(50) + "%", patternCaptor.getValue());
        }

        @Test
        @DisplayName("getAllAdmins uses ADMIN role and skips status filter")
        void adminsUsesAdminRole() {
            when(employeeRepository.searchByRole(
                    anyString(), anyString(), anyLong(), anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.getAllAdmins("", null, null, 0, 10);

            verify(employeeRepository).searchByRole(
                    eq("ADMIN"), eq(""), eq(0L), eq(Long.MAX_VALUE),
                    eq("%%"), any(Pageable.class));
        }

        @Test
        @DisplayName("status filter is uppercased and whitelisted")
        void statusFilterUppercased() {
            when(employeeRepository.searchByRole(
                    anyString(), anyString(), anyLong(), anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.getAllEmployees("", "active", null, null, 0, 10);

            verify(employeeRepository).searchByRole(
                    eq("EMPLOYEE"), eq("ACTIVE"), eq(0L), eq(Long.MAX_VALUE),
                    anyString(), any(Pageable.class));
        }
    }

    // ============================================================
    // updateEmployee
    // ============================================================
    @Nested
    @DisplayName("updateEmployee")
    class UpdateEmployee {

        @Test
        @DisplayName("missing id -> ResourceNotFound")
        void notFound() {
            Employee patch = new Employee();
            when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateEmployee(EMP_ID, patch));
        }

        @Test
        @DisplayName("updates only first/last/mobile and refreshes updatedOn")
        void updatesSafeFields() {
            Employee existing = employee("EMPLOYEE", "ACTIVE");
            long before = existing.getUpdatedOn();

            Employee patch = new Employee();
            patch.setFirstName("NewFirst");
            patch.setLastName("NewLast");
            patch.setMobile("1111111111");
            patch.setRole("ADMIN");            // attempt to escalate
            patch.setPassword("Hacked@123");   // attempt to overwrite

            when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.of(existing));
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Employee saved = service.updateEmployee(EMP_ID, patch);

            assertEquals("NewFirst", saved.getFirstName());
            assertEquals("NewLast", saved.getLastName());
            assertEquals("1111111111", saved.getMobile());
            assertEquals("EMPLOYEE", saved.getRole());               // unchanged
            assertEquals("Test@1234", saved.getPassword());          // unchanged
            assertTrue(saved.getUpdatedOn() >= before);
        }
    }

    // ============================================================
    // deleteEmployee
    // ============================================================
    @Nested
    @DisplayName("deleteEmployee")
    class DeleteEmployee {

        @Test
        @DisplayName("missing id -> ResourceNotFound, no delete")
        void notFound() {
            when(employeeRepository.existsById(EMP_ID)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteEmployee(EMP_ID));

            verify(employeeRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("existing id -> deletes")
        void success() {
            when(employeeRepository.existsById(EMP_ID)).thenReturn(true);

            service.deleteEmployee(EMP_ID);

            verify(employeeRepository).deleteById(EMP_ID);
        }
    }

    // ============================================================
    // generateEmpId
    // ============================================================
    @Nested
    @DisplayName("generateEmpId")
    class GenerateEmpId {

        @Test
        @DisplayName("starts at 001 when no employees exist")
        void firstEmployee() {
            when(employeeRepository.findAll()).thenReturn(List.of());

            String empId = service.generateEmpId();

            assertEquals("HRStack_" + Year.now().getValue() + "_IT_001", empId);
        }

        @Test
        @DisplayName("increments past highest existing sequence")
        void nextSequence() {
            String year = String.valueOf(Year.now().getValue());
            Employee e1 = new Employee();
            e1.setEmpId("HRStack_" + year + "_IT_005");
            Employee e2 = new Employee();
            e2.setEmpId("HRStack_" + year + "_IT_002");

            when(employeeRepository.findAll()).thenReturn(List.of(e1, e2));

            assertEquals("HRStack_" + year + "_IT_006", service.generateEmpId());
        }

        @Test
        @DisplayName("ignores non-matching ids")
        void ignoresJunk() {
            String year = String.valueOf(Year.now().getValue());
            Employee e1 = new Employee();
            e1.setEmpId("OTHER_PREFIX");
            Employee e2 = new Employee();
            e2.setEmpId("HRStack_" + year + "_IT_NOT_A_NUMBER");
            Employee e3 = new Employee();
            e3.setEmpId(null);

            when(employeeRepository.findAll()).thenReturn(List.of(e1, e2, e3));

            assertEquals("HRStack_" + year + "_IT_001", service.generateEmpId());
        }
    }
}