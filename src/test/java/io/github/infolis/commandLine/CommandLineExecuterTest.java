
package io.github.infolis.commandLine;

import io.github.infolis.InfolisBaseTest;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class CommandLineExecuterTest extends InfolisBaseTest {
    
    
    //TODO: paths in the json are absolut like the inputFiles
    @Ignore
    @Test
    public void test() throws FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, IOException {
    CommandLineExecuter.parseJson(getClass().getResource("/commandLine/algoDesc.json").getFile());

    }
}
