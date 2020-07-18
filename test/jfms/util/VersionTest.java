package jfms.util;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {
	@Test
	public void testcompare() {
		Assert.assertEquals(0, Version.compare("1", "1"));
		Assert.assertTrue( Version.compare("0", "1") < 0);
		Assert.assertTrue( Version.compare("0.1", "1") < 0);
		Assert.assertTrue( Version.compare("0", "1.1") < 0);

		Assert.assertTrue( Version.compare("1", "0") > 0);
		Assert.assertTrue( Version.compare("1.1", "0") > 0);
		Assert.assertTrue( Version.compare("1", "0.1") > 0);

		Assert.assertEquals(0, Version.compare("0.5", "0.5"));
		Assert.assertTrue( Version.compare("0.4", "0.5") < 0);
		Assert.assertTrue( Version.compare("0.5", "0.4") > 0);

		Assert.assertTrue( Version.compare("0", "0.4.1") < 0);
		Assert.assertTrue( Version.compare("0.4", "0.4.1") < 0);
		Assert.assertTrue( Version.compare("0.4.1", "0.4") > 0);
	}
}

