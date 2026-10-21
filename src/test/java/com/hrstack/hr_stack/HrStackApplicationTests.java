
package com.hrstack.hr_stack;

import com.hrstack.hr_stack.service.LoginChallengeCleanupScheduler;
import com.hrstack.hr_stack.service.SalarySlipStorageScheduler;
import com.hrstack.hr_stack.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import javax.sql.DataSource;
import java.sql.Connection;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@ActiveProfiles("test")
class HrStackApplicationTests {

	@MockitoBean
	LoginChallengeCleanupScheduler loginChallengeCleanupScheduler;

	@MockitoBean
	SalarySlipStorageScheduler salarySlipStorageScheduler;

	@MockitoBean
	LoginRateLimiter loginRateLimiter;

	@Test
	void contextLoads() {
	}

	@Autowired
	private DataSource dataSource;

	@Test
	void verifyTestDatabaseConnection() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			System.out.println("DATABASE NAME: "
					+ connection.getCatalog());
			System.out.println("DATABASE URL: "
					+ connection.getMetaData().getURL());
		}
	}
}
