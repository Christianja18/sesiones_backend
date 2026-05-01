package com.sesiones.sesiones_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "app.security.jwt.secret=test-secret-with-at-least-32-characters",
    "spring.main.lazy-initialization=true",
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
class SesionesBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}

