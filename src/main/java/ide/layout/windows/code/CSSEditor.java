package ide.layout.windows.code;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import lwjgui.font.FontMetaData;
import lwjgui.paint.Color;

public class CSSEditor extends AutoSyntaxCodeEditor {
	
	public CSSEditor() {
		// Check for keys pressed
		this.setOnKeyPressed((event) -> {
			if (event.isConsumed())
				return;

			if (!isEditing())
				return;

			if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
				String line = getLine(getCaretPosition() - 1);
				boolean function = line.contains("{");

				int amtTabs = countCharacters(line, '\t');
				int amtTabsNext = countCharacters(this.getLine(this.getCaretFromRowLine(this.getRowFromCaret(this.getCaretPosition())+1, 0)), '\t');
				int amtEndNext = countCharacters(this.getLine(this.getCaretFromRowLine(this.getRowFromCaret(this.getCaretPosition())+1, 0)), '}');
				String TABS = generateCharacters(amtTabs, '\t');
				
				// Indent next line
				if (amtTabs > 0) {
					insertText(getCaretPosition(), TABS);
					setCaretPosition(getCaretPosition() + TABS.length());
				}

				// Add end block
				if ( function ) {
					// Tab once more if entering block
					insertText(getCaretPosition(), "\t");
					setCaretPosition(getCaretPosition() + 1);

					// end
					if ( amtTabsNext <= amtTabs && (amtEndNext == 0 || (amtEndNext > 0 && amtTabsNext != amtTabs)) )
						insertText(getCaretPosition(), "\n" + TABS + "}");
				}
				
				event.consume();
			}
		});
	}
	
	@Override
	protected Map<String, HighlightData> getRegexMap() {
		Map<String, HighlightData> regex = new HashMap<>();
		
		regex.put("COMMENT", new HighlightData()
								.priority(50)
								.regex("/\\*([^*]|\\*[^/])*\\*/")
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
