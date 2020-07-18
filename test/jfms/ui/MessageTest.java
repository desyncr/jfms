package jfms.ui;

import org.junit.Assert;
import org.junit.Test;

public class MessageTest {
	@Test
	public void testFromShort() {
		Message message = new Message();

		message.setFrom(null);
		Assert.assertEquals(null, message.getFromShort());

		message.setFrom("1234");
		Assert.assertEquals("1234", message.getFromShort());

		message.setFrom("1234@SSK");
		Assert.assertEquals("1234", message.getFromShort());

		message.setFrom("12@34@SSK");
		Assert.assertEquals("12@34", message.getFromShort());
	}
}
