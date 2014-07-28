package es.cbgp.old2sadi.main;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

/**
 * Static Utils
 * 
 * @author Alejandro Rodríguez González - Centre for Biotechnology and Plant
 *         Genomics
 * 
 */
public class StaticUtils {

	/**
	 * Method to convert seconds to hours, minutes, seconds.
	 * 
	 * @param biggy
	 *            Receives the seconds.
	 * @return The value.
	 */
	public static String convertSecondsToTime(BigDecimal biggy) {
		long longVal = biggy.longValue();
		int hours = (int) longVal / 3600;
		int remainder = (int) longVal - hours * 3600;
		int mins = remainder / 60;
		remainder = remainder - mins * 60;
		int secs = remainder;
		return hours + " hours, " + mins + " mins, " + secs + " secs.";
	}

	/**
	 * Método para borrar una extensión en una cadena que contenga un fichero y
	 * su extensión.
	 * 
	 * @param name
	 *            Recibe la cadena.
	 * @return Devuelve otra cadena.
	 */
	public static String removeExtension(String name) {
		String ret = "";
		for (int i = 0; i < name.lastIndexOf('.'); i++) {
			ret += name.charAt(i);
		}
		return ret;
	}

	/**
	 * Method to check if the string is empty ("" or null)
	 * 
	 * @param str
	 *            Receives the string.
	 * @return Returns a boolean.
	 */
	public static boolean isEmpty(String str) {
		return ((str == null) || (str.trim().equals("")));
	}

	/**
	 * Method to, given a concrete string, get all the data starting from the
	 * end until we found the first slash. For example. Given string:
	 * testing/whatever we want to obtain "whatever".
	 * 
	 * @param s
	 *            Receives the string.
	 * @return Return the final string.
	 */
	public static String getUntilFirstSlashBackwards(String s) {
		String tmpString = "";
		for (int i = s.length() - 1; i >= 0; i--) {
			if (s.charAt(i) != '/') {
				tmpString += s.charAt(i);
			} else {
				break;
			}
		}
		String finalString = "";
		for (int i = tmpString.length() - 1; i >= 0; i--) {
			finalString += tmpString.charAt(i);
		}
		return finalString;
	}

	/**
	 * Method to know if all the characteres of a given String are numbers.
	 * 
	 * @param s
	 *            Receives the string.
	 * @return Return true or false.
	 */
	public static boolean isAllNumbers(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			try {
				Integer.parseInt(Character.toString(c));
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public static String loadFileToString(String f) throws Exception {
		String finalString = "";
		BufferedReader bL = new BufferedReader(new FileReader(new File(f)));
		while (bL.ready()) {
			String rd = bL.readLine();
			finalString += rd + "\r\n";
		}
		bL.close();
		return finalString;
	}

	public static String getXMLSChemaType(String uri) {
		String parts[] = uri.split("#");
		if (parts.length == 2) {
			return parts[1];
		}
		return "Unknown datatype";
	}

	public static String serialize(Object ob) throws Exception {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream so = new ObjectOutputStream(bo);
		so.writeObject(ob);
		so.flush();
		String serializedObject = bo.toString();
		return serializedObject;
	}

	public static Object deSerialize(String ob) throws Exception {
		byte b[] = ob.getBytes();
		ByteArrayInputStream bi = new ByteArrayInputStream(b);
		ObjectInputStream si = new ObjectInputStream(bi);
		Object obj = si.readObject();
		return obj;
	}

	/**
	 * Method to check if an arbitrary number of parameters are not null (then, all of them are valid).
	 * 
	 * In the case that any of the objects is a String, we check if it is not empty.
	 * @param objects Receive the objects.
	 * @return A boolean.
	 */
	public static boolean areValid(Object ...objects) {
		for (Object obj: objects) {
			if (obj instanceof String) {
				if (isEmpty((String)obj)) {
					return false;
				}
			}
			else {
				if (obj == null) {
					return false;
				}
			}
		}
		return true;
	}
}