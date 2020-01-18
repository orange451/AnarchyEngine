package ide.layout.windows.code;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import lwjgui.font.FontMetaData;
import lwjgui.font.FontStyle;
import lwjgui.paint.Color;

public class LuaEditor extends AutoSyntaxCodeEditor {
	
	public LuaEditor() {
		// Check for keys pressed
		this.setOnKeyPressed((event) -> {
			if (event.isConsumed())
				return;

			if (!isEditing())
				return;

			if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
				String line = getLine(getCaretPosition() - 1);
				String lineWithoutSpecial = line.replaceAll("[^a-zA-Z0-9]+", " ");
				boolean ifThen = lineWithoutSpecial.contains("if ") && lineWithoutSpecial.contains(" then");
				boolean whileDo = lineWithoutSpecial.contains("while ") && lineWithoutSpecial.contains(" do");
				boolean forDo = lineWithoutSpecial.contains("for ") && lineWithoutSpecial.contains(" do");
				boolean function = line.contains("function(") && line.contains(")");
				int closeParenthesisDifference = (int) (line.chars().filter(ch -> ch == '(').count() - line.chars().filter(ch -> ch == ')').count());

				int amtTabs = countCharacters(line, '\t');
				int amtTabsNext = countCharacters(this.getLine(this.getCaretFromRowLine(this.getRowFromCaret(this.getCaretPosition())+1, 0)), '\t');
				String TABS = generateCharacters(amtTabs, '\t');
				String CLOSE_PAREN = generateCharacters(closeParenthesisDifference, ')');
				
				// Indent next line
				if (amtTabs > 0) {
					insertText(getCaretPosition(), TABS);
					setCaretPosition(getCaretPosition() + TABS.length());
				}

				// Add end block
				if ((ifThen || function || forDo || whileDo)) {
					// Tab once more if entering block
					insertText(getCaretPosition(), "\t");
					setCaretPosition(getCaretPosition() + 1);

					// end
					if ( amtTabsNext <= amtTabs )
						insertText(getCaretPosition(), "\n" + TABS + "end" + CLOSE_PAREN);
				}
				
				event.consume();
			}
		});
	}
	
	@Override
	protected Map<String, HighlightData> getRegexMap() {
		Map<String, HighlightData> regex = new HashMap<>();
		
		regex.put("KEYWORD", new HighlightData()
								.priority(50)
								.regex("\\b("
										+ String.join("|", new String[] {
											// Logical keywords
											"and",
											"end",
											"in",
											"repeat",
											"break",
											"false",
											"local",
											"return",
											"do",
											"for",
											"nil",
											"then",
											"else",
											"function",
											"not",
											"true",
											"elseif",
											"if",
											"or",
											"until",
											"while",
							
											// functions/variables
											"script",
											"game",
											"Instance",
											"math",
											"wait",
											"print",
											"tick",
											"spawn",
											"tostring",
											"Enum",
											"_G",
							
											// Datatypes
											"Vector3",
											"Color3",
											"Color4",
											"Ray",
											"Matrix4",
											"Vector2"
										})
										+ ")\\b")
								.fontMeta(
										new FontMetaData().color(new Color("#2239a8")).style(FontStyle.BOLD)
								));
		
		regex.put("NUMBER", new HighlightData()
								.priority(20)
								.regex(" ^\\d*\\.\\d+" + "|" + "\\d+\\.\\d" + "|" + "\\d+")
								.fontMeta(
										new FontMetaData().color(new Color("#2B957F"))
								));
		
		regex.put("COMMENT", new HighlightData()
								.priority(100)
								.regex("--(\\[\\[)(.|\\R)*?(\\]\\])" + "|" + "--[^\n]*")
								.fontMeta(
										new FontMetaData().color(new Color("#007F00"))
								));
		
		regex.put("LOGIC", new HighlightData()
								.priority(1)
								.regex("\\p{Punct}")
								.fontMeta(
										new FontMetaData().color(new Color("#7F7F00"))
								));
		
		regex.put("PAREN", new HighlightData()
								.priority(1)
								.regex("\\(" + "|" + "\\)")
								.fontMeta(
										new FontMetaData().color(new Color("#7F7F00"))
							));
		
		regex.put("BRACKET", new HighlightData()
								.priority(1)
								.regex("\\[|\\]")
								.fontMeta(
										new FontMetaData().color(new Color("#7F7F00"))
							));
		
		regex.put("STRING", new HighlightData()
								.priority(40)
								.regex("(\"(.*?)\"|'(.*?)'|(?s)(\\[\\[)(.*?)(\\]\\]))")
								.fontMeta(
										new FontMetaData().color(new Color("#800080"))
								));
		
		return regex;
	}
}
