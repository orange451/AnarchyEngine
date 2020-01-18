package ide.layout.windows.code;

public abstract class AutoSyntaxCodeEditor extends SearchableCodeEditor {
	/**
	 * Returns the amount of tabs that are at the start of the specified string.
	 * @param t
	 * @return
	 */
	protected int countCharacters(String t, char ch) {
		int a = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == ch)
				a++;
			else
				break;
		}

		return a;
	}

	/**
	 * Method to generate n amount of characters as a string.
	 * @param amt
	 * @param c
	 * @return
	 */
	protected String generateCharacters(int amt, char c) {
		StringBuilder t = new StringBuilder();
		for (int i = 0; i < amt; i++) {
			t.append("" + c);
		}
		return t.toString();
	}
}
