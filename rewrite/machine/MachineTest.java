package machine;

import java.io.FileNotFoundException;

import filesystem.CreateFS;

import junit.framework.TestCase;


public class MachineTest extends TestCase{

	public void testMachineStart() throws Exception{
		//Config.load("/Users/pauljohnson/ConcurrentTeaching/eclipse/nachos-rewrite/rewriteconf/nachos.conf");
		//CreateFS.main(null);
		
		Machine.main(new String[]{});
	}
}
