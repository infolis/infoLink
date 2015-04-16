package io.github.infolis.ws.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import io.github.infolis.ws.server.InfolisConfig;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;


public class InfolisConfigTest {
	
	@Test
	public void test() throws IOException {
//		assertThat(conf.getFileSavePath().toString(), is("/tmp/infolis-ws"));
		assertThat(Files.exists(InfolisConfig.getFileSavePath()), is(true));
	}

}
