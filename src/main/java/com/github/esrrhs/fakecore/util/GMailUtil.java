package com.github.esrrhs.fakecore.util;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GMailUtil
{
	private static Logger log = LoggerFactory.getLogger(GMailUtil.class);

	private static String USER_NAME = "xxxxx@gmail.com"; // GMail user name (just the part before "@gmail.com")
	private static String PASSWORD = "xxxxx"; // GMail password

	public static void main(String[] args)
	{
		String subject = "Java send mail example";
		String body = "Welcome to JavaMail!";

		sendFromGMail(Arrays.asList("esrrhs@163.com"), subject, body);
	}

	public static boolean sendFromGMail(String to, String subject, String body)
	{
		return sendFromGMail(Arrays.asList(to), subject, body);
	}

	public static boolean sendFromGMail(List<String> to, String subject, String body)
	{
		try
		{
			Properties props = System.getProperties();
			String host = "smtp.gmail.com";
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", host);
			props.put("mail.smtp.user", USER_NAME);
			props.put("mail.smtp.password", PASSWORD);
			props.put("mail.smtp.port", "587");
			props.put("mail.smtp.auth", "true");

			Session session = Session.getDefaultInstance(props);
			MimeMessage message = new MimeMessage(session);

			message.setFrom(new InternetAddress(USER_NAME));
			InternetAddress[] toAddress = new InternetAddress[to.size()];

			// To get the array of addresses
			for (int i = 0; i < to.size(); i++)
			{
				toAddress[i] = new InternetAddress(to.get(i));
			}

			for (int i = 0; i < toAddress.length; i++)
			{
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
			}

			message.setSubject(subject);
			message.setText(body);
			Transport transport = session.getTransport("smtp");
			transport.connect(host, USER_NAME, PASSWORD);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();

			return true;
		}
		catch (Exception e)
		{
			log.error("sendFromGMail ", e);
			return false;
		}
	}
}
