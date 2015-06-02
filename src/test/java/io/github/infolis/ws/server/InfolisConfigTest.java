package io.github.infolis.ws.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.github.infolis.infolink.luceneIndexing.InfolisBaseTest;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfolisConfigTest extends InfolisBaseTest {

	Logger log = LoggerFactory.getLogger(InfolisConfigTest.class);

	@Test
	public void test() throws IOException {
		// assertThat(conf.getFileSavePath().toString(), is("/tmp/infolis-ws"));
		assertThat(Files.exists(InfolisConfig.getFileSavePath()), is(true));
	}

}
