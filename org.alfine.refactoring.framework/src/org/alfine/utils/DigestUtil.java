package org.alfine.utils;

import java.security.MessageDigest;

import jakarta.xml.bind.DatatypeConverter;

public class DigestUtil {
	public static String md5(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes());
			return DatatypeConverter.printHexBinary(md.digest());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
