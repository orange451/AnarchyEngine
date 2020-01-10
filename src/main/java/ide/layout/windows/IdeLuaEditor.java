/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package ide.layout.windows;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.glfw.GLFW;

import engine.lua.type.object.ScriptBase;
import ide.layout.IdePane;
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
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.floating.FloatingPane;
import lwjgui.style.BorderStyle;
import lwjgui.style.BoxShadow;
import lwjgui.theme.Theme;

public class IdeLuaEditor extends IdePane {
	private ScriptBase inst;
	private CodeArea code;
	private SearchBar searchbar;
	private String searchTerm = "";

	public IdeLuaEditor(ScriptBase script) {
		super(script.getName() + ".lua", true);

		code = new CodeArea();
		code.setFontSize(16);
		code.setFont(Font.COURIER);
		code.setFillToParentHeight(true);
		code.setFillToParentWidth(true);
		this.getChildren().add(code);

		code.setOnKeyPressed((event) -> {
			if (event.isConsumed())
				return;

			if (IdeLuaEditor.this.isDescendentSelected()) {
				if (event.getKey() == GLFW.GLFW_KEY_F && event.isCtrlDown) {
					System.out.println("Opening searchbar... " + this.searchbar);
					if (IdeLuaEditor.this.searchbar == null) {
						IdeLuaEditor.this.getChildren().add(IdeLuaEditor.this.searchbar = new SearchBar());
					} else {
						if (this.searchbar.isSelected() || this.searchbar.isDescendentSelected()) {
							IdeLuaEditor.this.getChildren().remove(IdeLuaEditor.this.searchbar);
							IdeLuaEditor.this.searchbar = null;
						}
					}

					if (searchbar != null) {
						if (code.getSelectedText().length() > 0) {
							searchTerm = code.getSelectedText();
							searchbar.input.setText(searchTerm);
						}

						this.cached_context.setSelected(this.searchbar.input);
						this.searchbar.input.selectAll();
					}
				}
			}

			if (!code.isEditing())
				return;

			if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
				String line = code.getLine(code.getCaretPosition() - 1);
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
					code.insertText(code.getCaretPosition(), TABS);
					code.setCaretPosition(code.getCaretPosition() + TABS.length());
				}

				// Add end block
				if (ifThen || function || forDo || whileDo) {
					// Tab once more if entering block
					code.insertText(code.getCaretPosition(), "\t");
					code.setCaretPosition(code.getCaretPosition() + 1);

					// end
					code.insertText(code.getCaretPosition(), "\n" + TABS + "end" + CLOSE_PAREN);
				}
			}
		});

		// Syntax highlighting variables
		final String KEYWORD_PATTERN = "\\b(" + String.join("|", new String[] {
				// Logical keywords
				"and", "end", "in", "repeat", "break", "false", "local", "return", "do", "for", "nil", "then", "else",
				"function", "not", "true", "elseif", "if", "or", "until", "while",

				// functions/variables
				"script", "game", "Instance", "math", "wait", "print", "tick", "spawn", "tostring", "Enum", "_G",

				// Datatypes
				"Vector3", "Color3", "Matrix4", "Vector2" }) + ")\\b";

		final String LOGIC_PATTERN = "\\p{Punct}";
		final String NUMBER_PATTERN = "^\\d*\\.\\d+" + "|" + "\\d+\\.\\d" + "|" + "\\d+";
		final String PAREN_PATTERN = "\\(" + "|" + "\\)";
		final String BRACKET_PATTERN = "\\[|\\]";
		final String STRING_PATTERN = "(\"(.*?)\"|'(.*?)'|(?s)(\\[\\[)(.*?)(\\]\\]))";
		final String COMMENT_PATTERN = "--(\\[\\[)(.|\\R)*?(\\]\\])" + "|" + "--[^\n]*";

		Pattern PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")" + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
				+ "|(?<STRING>" + STRING_PATTERN + ")" + "|(?<LOGIC>" + LOGIC_PATTERN + ")" + "|(?<PAREN>"
				+ PAREN_PATTERN + ")" + "|(?<BRACKET>" + BRACKET_PATTERN + ")" + "|(?<NUMBER>" + NUMBER_PATTERN + ")");

		// Do syntax highlighting
		code.setOnTextChange((event) -> {
			code.resetHighlighting();
			if (Theme.current() instanceof lwjgui.theme.ThemeWhite)
				code.setFontFill(Color.BLACK);

			String text = code.getText();
			Matcher matcher = PATTERN.matcher(text);

			// Iterate through pattern recognition, apply highlighting.
			while (matcher.find()) {
				int start = matcher.start();
				int end = matcher.end() - 1;

				// Apply syntax
				if (matcher.group("KEYWORD") != null) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#2239a8")).style(FontStyle.BOLD));
				}

				if (matcher.group("COMMENT") != null) {
					code.setHighlighting(start, end, new FontMetaData().color(new Color("#007F00")));
				}

				if (matcher.group("STRING") != null) {
					code.setHighlighting(start, end, new FontMetaData().color(new Color("#800080")));
				}

				if (matcher.group("LOGIC") != null || matcher.group("PAREN") != null
						|| matcher.group("BRACKET") != null) {
					code.setHighlighting(start, end, new FontMetaData().color(new Color("#7F7F00")));
				}

				if (matcher.group("NUMBER") != null) {
					code.setHighlighting(start, end, new FontMetaData().color(new Color("#2B957F")));
				}
			}
		});

		code.setText(script.getSource());
		this.inst = script;
	}

	private long lastSave = System.currentTimeMillis();

	@Override
	protected void position(Node parent) {
		super.position(parent);

		if (System.currentTimeMillis() - lastSave > 500) {
			try {
				inst.setSource(code.getText());
				lastSave = System.currentTimeMillis();
			} catch (Exception e) {
				inst = null;
				this.dockedTo.undock(this);
			}
		}
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		if (inst == null)
			return;
		inst.setSource(code.getText());
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
				searchTerm = this.input.getText();
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
			this.setStylesheet("" + ".SearchButton {" + "		background-color:transparent;"
					+ "		border-radius:12px;" + "		width:20px;" + "		height:20px;" + "}" + ""
					+ ".SearchButton:hover {" + "		background-color:lightgray;" + "}");

			// Close Button logic
			this.closeButton.setOnMouseClicked((event) -> {
				IdeLuaEditor.this.getChildren().remove(IdeLuaEditor.this.searchbar);
				IdeLuaEditor.this.searchbar = null;
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
			this.setLocalPosition(code, code.getViewportWidth() - this.getWidth() - 2, 2);

			// Reset the parent so it's not parented to code window
			this.parent = IdeLuaEditor.this;
		}
	}
}
