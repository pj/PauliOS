// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package machine;

import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StreamTokenizer;


/**
 * Provides routines to access the Nachos configuration.
 */
public final class Config {
	/**
	 * Load configuration information from the specified file. Must be called
	 * before the Nachos security manager is installed.
	 * 
	 * @param fileName
	 *            the name of the file containing the configuration to use.
	 */
	public static void load(String fileName) {
		System.out.print(" config");

		Lib.assert_(!loaded);
		loaded = true;

		configFile = fileName;

		try {
			config = new HashMap();

			File file = new File(configFile);
			Reader reader = new FileReader(file);
			StreamTokenizer s = new StreamTokenizer(reader);

			s.resetSyntax();
			s.whitespaceChars(0x00, 0x20);
			s.wordChars(0x21, 0xFF);
			s.eolIsSignificant(true);
			s.commentChar('#');
			s.quoteChar('"');

			int line = 1;

			s.nextToken();

			while (true) {
				if (s.ttype == StreamTokenizer.TT_EOF)
					break;

				if (s.ttype == StreamTokenizer.TT_EOL) {
					line++;
					s.nextToken();
					continue;
				}

				if (s.ttype != StreamTokenizer.TT_WORD)
					loadError(line);

				String key = s.sval;

				if (s.nextToken() != StreamTokenizer.TT_WORD
						|| !s.sval.equals("="))
					loadError(line);

				if (s.nextToken() != StreamTokenizer.TT_WORD && s.ttype != '"')
					loadError(line);

				String value = s.sval;

				// ignore everything after first string
				while (s.nextToken() != StreamTokenizer.TT_EOL
						&& s.ttype != StreamTokenizer.TT_EOF)
					;

				if (config.get(key) != null)
					loadError(line);

				config.put(key, value);
				line++;
			}
		} catch (Throwable e) {
			System.err.println("Error loading " + configFile);
			System.exit(1);
		}
	}

	private static void loadError(int line) {
		System.err.println("Error in " + configFile + " line " + line);
		System.exit(1);
	}

	private static void configError(String message) {
		System.err.println("");
		System.err.println("Error in " + configFile + ": " + message);
		System.exit(1);
	}

	/**
	 * Get the value of a key in <tt>nachos.conf</tt>.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the value of the specified key, or <tt>null</tt> if it is not
	 *         present.
	 */
	public static String getString(String key) {
		return (String) config.get(key);
	}

	/**
	 * Get the value of an integer key in <tt>nachos.conf</tt>.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the value of the specified key.
	 */
	public static int getInteger(String key) {
		try {
			String value = getString(key);
			if (value == null)
				configError("missing int " + key);

			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			configError(key + " should be an integer");

			Lib.assertNotReached();
			return 0;
		}
	}

	/**
	 * Get the value of a double key in <tt>nachos.conf</tt>.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the value of the specified key.
	 */
	public static double getDouble(String key) {
		try {
			String value = getString(key);
			if (value == null)
				configError("missing double " + key);

			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			configError(key + " should be a double");

			Lib.assertNotReached();
			return 0;
		}
	}

	/**
	 * Get the value of a boolean key in <tt>nachos.conf</tt>.
	 * 
	 * @param key
	 *            the key to look up.
	 * @return the value of the specified key.
	 */
	public static boolean getBoolean(String key) {
		String value = getString(key);

		if (value == null)
			configError("missing boolean " + key);

		if (value.equals("1") || value.toLowerCase().equals("true")) {
			return true;
		} else if (value.equals("0") || value.toLowerCase().equals("false")) {
			return false;
		} else {
			configError(key + " should be a boolean");

			Lib.assertNotReached();
			return false;
		}
	}

	private static boolean loaded = false;
	private static String configFile;
	private static HashMap config;
}
