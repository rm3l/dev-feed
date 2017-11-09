package org.rm3l.tools.reverseproxy

import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class ReverseProxyApplicationTests {

	private val logger = LoggerFactory.getLogger(ReverseProxyApplicationTests::class.java)

	@Test
	fun contextLoads() = logger.info("Hooray! Application Context successfully loaded.")

}
