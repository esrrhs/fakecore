package com.github.esrrhs.fakecore.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeteaseMailUtil
{
	private static Logger log = LoggerFactory.getLogger(NeteaseMailUtil.class);

	private static String from = "xxx@163.com"; // 发件人邮箱地址
	private static String password = "xxxxx"; // 发件人邮箱客户端授权码

	public static void main(String[] args)
	{
		String subject = "Java send mail example";
		String body = "Welcome to JavaMail!";

		sendFromNeteaseMail("esrrhs@163.com", subject, body);
	}

	public static boolean sendFromNeteaseMail(String to, String title, String text)
	{
		try
		{
			Properties props = new Properties();
			props.put("username", from);
			props.put("password", password);
			props.put("mail.transport.protocol", "smtp");
			props.put("mail.smtp.host", "smtp.163.com");
			props.put("mail.smtp.port", "25");

			Session mailSession = Session.getDefaultInstance(props);

			Message msg = new MimeMessage(mailSession);
			msg.setFrom(new InternetAddress(from));
			msg.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			msg.setSubject(title);
			msg.setContent(text, "text/html;charset=UTF-8");

			msg.saveChanges();

			Transport transport = mailSession.getTransport("smtp");
			transport.connect(props.getProperty("mail.smtp.host"), props.getProperty("username"),
					props.getProperty("password"));
			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();
		}
		catch (Exception e)
		{
			log.error("sendFromNeteaseMail ", e);
			return false;
		}
		return true;
	}
}
