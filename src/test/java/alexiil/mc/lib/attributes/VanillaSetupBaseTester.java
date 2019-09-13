package alexiil.mc.lib.attributes;

import java.io.InputStream;
import java.io.PrintStream;

import org.junit.BeforeClass;

import net.minecraft.Bootstrap;

public class VanillaSetupBaseTester {

    @BeforeClass
    public static void init() {
        System.out.println("INIT");
        PrintStream sysOut = System.out;
        InputStream sysIn = System.in;

        Bootstrap.initialize();

        System.setIn(sysIn);
        System.setOut(sysOut);
    }

}
