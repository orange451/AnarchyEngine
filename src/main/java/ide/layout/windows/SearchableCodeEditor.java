package ide.layout.windows;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.glfw.GLFW;

import lwjgui.LWJGUI;
import lwjgui.font.Font;
import lwjgui.font.FontMetaData;
import lwjgui.font.FontStyle;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.control.CodeArea;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.floating.FloatingPane;
import lwjgui.style.BorderStyle;
import lwjgui.style.BoxShadow;
import lwjgui.theme.Theme;

public abstract class SearchableCodeEditor extends CodeArea {
	private SearchBar searchbar;
	private String searchTerm = "";
	private Pattern regexHighlightPattern;
	
	public SearchableCodeEditor() {
		this.setFontSize(16);
		this.setFont(Font.COURIER);
		this.setFillToParentHeight(true);
		this.setFillToParentWidth(true);
		
		this.setOnKeyPressed((event) -> {
			if (event.isConsumed())
				return;

			if (SearchableCodeEditor.this.getParent().isDescendentSelected()) {
				if (event.getKey() == GLFW.GLFW_KEY_F && event.isCtrlDown) {
					System.out.println("Opening searchbar... " + this.searchbar);
					if (searchbar == null) {
						((Pane)SearchableCodeEditor.this.getParent()).getChildren().add(searchbar = new SearchBar());
					} else {
						if (this.searchbar.isSelected() || this.searchbar.isDescendentSelected()) {
							((Pane)SearchableCodeEditor.this.getParent()).getChildren().remove(searchbar);
							searchbar = null;
						}
					}

					if (searchbar != null) {
						if (getSelectedText().length() > 0) {
							searchbar.input.setText(getSelectedText());
							setSearchTerm(getSelectedText());
						}

						this.cached_context.setSelected(this.searchbar.input);
						this.searchbar.input.selectAll();
					}
				}
			}

			if (!isEditing())
				return;

			if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
				String line = getLine(getCaretPosition() - 1);
				String lineWithoutSpecial = line.replaceAll("[^a-zA-Z0-9]+", " ");
				boolean ifThen = lineWithoutSpecial.contains("if ") && lineWithoutSpecial.contains(" then");
				boolean whileDo = lineWithoutSpecial.contains("while ") && lineWithoutSpecial.contains(" do");
				boolean forDo = lineWithoutSpecial.contains("for ") && lineWithoutSpecial.contains(" do");
				boolean function = line.contains("function(") && line.contains(")");
				int closeParenthesisDifference = (int) (line.chars().filter(ch -> ch == '(').count()
						- line.chars().filter(ch -> ch == ')').count());

				int amtTabs = getTabs(line);
				String TABS = generateCharacters(amtTabs, '\t');
				String CLOSE_PAREN = generateCharacters(closeParenthesisDifference, ')');

				// Indent next line
				if (amtTabs > 0) {
					insertText(getCaretPosition(), TABS);
					setCaretPosition(getCaretPosition() + TABS.length());
				}

				// Add end block
				if (ifThen || function || forDo || whileDo) {
					// Tab once more if entering block
					insertText(getCaretPosition(), "\t");
					setCaretPosition(getCaretPosition() + 1);

					// end
					insertText(getCaretPosition(), "\n" + TABS + "end" + CLOSE_PAREN);
				}
			}
		});
		
		this.regeneratePattern();
	}
	
	private void setSearchTerm(String term) {
		this.searchTerm = term;
		this.regeneratePattern();
		this.applyHighlighting();
	}
	
	private void regeneratePattern() {
		this.regexHighlightPattern = generatePattern();
	}

	private Pattern generatePattern() {
		// Syntax highlighting variables
		final String KEYWORD_PATTERN = "\\b("
				+ String.join("|", new String[] {
					// Logical keywords
					"and", "end", "in", "repeat", "break", "false", "local", "return", "do", "for", "nil", "then", "else",
					"function", "not", "true", "elseif", "if", "or", "until", "while",
	
					// functions/variables
					"script", "game", "Instance", "math", "wait", "print", "tick", "spawn", "tostring", "Enum", "_G",
	
					// Datatypes
					"Vector3", "Color3", "Color4", "Ray", "Matrix4", "Vector2" })
				+ ")\\b";

		final String LOGIC_PATTERN = "\\p{Punct}";
		final String NUMBER_PATTERN = "^\\d*\\.\\d+" + "|" + "\\d+\\.\\d" + "|" + "\\d+";
		final String PAREN_PATTERN = "\\(" + "|" + "\\)";
		final String BRACKET_PATTERN = "\\[|\\]";
		final String STRING_PATTERN = "(\"(.*?)\"|'(.*?)'|(?s)(\\[\\[)(.*?)(\\]\\]))";
		final String COMMENT_PATTERN = "--(\\[\\[)(.|\\R)*?(\\]\\])" + "|" + "--[^\n]*";
		String SEARCH_PATTERN = searchTerm==null?"":searchTerm;
		SEARCH_PATTERN = SEARCH_PATTERN.replaceAll("[\\W]", "\\\\$0");
		
		String combinedRegex = 
				"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
				+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
				+ "|(?<STRING>" + STRING_PATTERN + ")"
				+ "|(?<LOGIC>" + LOGIC_PATTERN + ")"
				+ "|(?<PAREN>" + PAREN_PATTERN + ")"
				+ "|(?<BRACKET>" + BRACKET_PATTERN + ")"
				+ "|(?<NUMBER>" + NUMBER_PATTERN + ")";
		combinedRegex += "|(?<SEARCH>" + SEARCH_PATTERN + ")";
		
		Pattern PATTERN = Pattern.compile(combinedRegex);
		return PATTERN;
	}
	
	private int getTabs(String t) {
		int a = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);
			if (c == '\t')
				a++;
			else
				break;
		}

		return a;
	}

	private String generateCharacters(int amt, char c) {
		StringBuilder t = new StringBuilder();
		for (int i = 0; i < amt; i++) {
			t.append("" + c);
		}
		return t.toString();
	}
	
	class SearchBar extends FloatingPane {

		private HBox internalBox;
		private StackPane closeButton;
		private TextField input;

		public SearchBar() {
			this.setPrefSize(250, 0);
			this.setBackgroundLegacy(Theme.current().getBackground());
			this.setBorderRadii(2);
			this.setBorderWidth(1);
			this.setBorderStyle(BorderStyle.SOLID);

			this.getBoxShadowList().add(new BoxShadow(0, 2, 8, -4));
			this.setPadding(new Insets(5));

			this.internalBox = new HBox();
			this.internalBox.setSpacing(4);
			this.internalBox.setFillToParentWidth(true);
			this.getChildren().add(this.internalBox);

			// Search bar
			this.input = new TextField();
			this.input.setFillToParentWidth(true);
			this.input.setStyle("border-style:none; box-shadow: none;");
			this.input.setText(searchTerm);
			this.internalBox.getChildren().add(input);

			// Searchbar logic
			this.input.setOnTextChange((event) -> {
				setSearchTerm(this.input.getText());
			});

			// Vertical Rule
			StackPane vr = new StackPane();
			vr.setBackgroundLegacy(Theme.current().getSelectionPassive());
			vr.setPrefWidth(1);
			vr.setFillToParentHeight(true);
			this.internalBox.getChildren().add(vr);

			// up button
			{
				this.closeButton = new StackPane();
				this.closeButton.setAlignment(Pos.CENTER);
				this.closeButton.getClassList().add("SearchButton");
				Label l = new Label("\u02C4");
				l.setMouseTransparent(true);
				l.setFont(Font.COURIER);
				l.setFontSize(18);
				this.closeButton.getChildren().add(l);
				this.internalBox.getChildren().add(this.closeButton);
			}

			// down button
			{
				this.closeButton = new StackPane();
				this.closeButton.setAlignment(Pos.CENTER);
				this.closeButton.getClassList().add("SearchButton");
				Label l = new Label("\u02C5");
				l.setMouseTransparent(true);
				l.setFont(Font.COURIER);
				l.setFontSize(18);
				this.closeButton.getChildren().add(l);
				this.internalBox.getChildren().add(this.closeButton);
			}

			// Close button
			{
				this.closeButton = new StackPane();
				this.closeButton.setAlignment(Pos.CENTER);
				this.closeButton.getClassList().add("SearchButton");
				Label l = new Label("\u2715");
				l.setMouseTransparent(true);
				l.setFont(Font.COURIER);
				l.setFontSize(18);
				this.closeButton.getChildren().add(l);
				this.internalBox.getChildren().add(this.closeButton);
			}

			// searchbar CSS
			this.setStylesheet(""
					+ ".SearchButton {"
					+ "		background-color:transparent;"
					+ "		border-radius:12px;"
					+ "		width:20px;"
					+ "		height:20px;" + "}"
					+ ""
					+ ".SearchButton:hover {"
					+ "		background-color:rgba(200,200,200,0.5);"
					+ "}"
			);

			// Close Button logic
			this.closeButton.setOnMouseClicked((event) -> {
				((Pane)SearchableCodeEditor.this.getParent()).getChildren().remove(searchbar);
				searchbar = null;
			});

			// Force our input to be selected
			LWJGUI.getCurrentContext().setSelected(input);
		}

		@Override
		public void position(Node parent) {
			super.position(parent);

			// Update outline color
			this.setBorderColor((this.isSelected() || this.isDescendentSelected()) ? Theme.current().getSelection()
					: Theme.current().getControlOutline());

			// This positions the element relative to the code area
			this.setLocalPosition(SearchableCodeEditor.this, SearchableCodeEditor.this.getViewportWidth() - this.getWidth() - 2, 2);

			// Reset the parent so it's not parented to code window
			this.parent = SearchableCodeEditor.this.getParent();
		}
	}

	public Pattern getRegexPattern() {
		return this.regexHighlightPattern;
	}

	protected void applyHighlighting() {
		String text = this.getText();
		Matcher matcher = this.getRegexPattern().matcher(text);
		
		this.resetHighlighting();

		// Iterate through pattern recognition, apply highlighting.
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end() - 1;
			
			if (matcher.group("SEARCH") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(Color.BLACK).background(Color.YELLOW.alpha(0.75f)));
			}

			if (matcher.group("KEYWORD") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(new Color("#2239a8")).style(FontStyle.BOLD));
			}

			if (matcher.group("COMMENT") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(new Color("#007F00")));
			}

			if (matcher.group("STRING") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(new Color("#800080")));
			}

			if (matcher.group("LOGIC") != null || matcher.group("PAREN") != null || matcher.group("BRACKET") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(new Color("#7F7F00")));
			}

			if (matcher.group("NUMBER") != null) {
				this.setHighlighting(start, end, new FontMetaData().color(new Color("#2B957F")));
			}
		}
	}
}
