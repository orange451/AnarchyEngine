package ide.layout.windows;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import engine.lua.type.object.ScriptBase;
import ide.layout.IdePane;
import lwjgui.font.Font;
import lwjgui.font.FontMetaData;
import lwjgui.font.FontStyle;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.control.CodeArea;

public class IdeLuaEditor extends IdePane {
	private ScriptBase inst;
	private CodeArea code;
	
	public IdeLuaEditor(ScriptBase script) {
		super(script.getName()+".lua", true);
		
		code = new CodeArea();
		code.setFontSize(16);
		code.setFont(Font.COURIER);
		code.setFillToParentHeight(true);
		code.setFillToParentWidth(true);
		this.getChildren().add(code);
		
		// Syntax highlighting variables
		final String KEYWORD_PATTERN = "\\b(" + String.join("|", new String[] {
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
				
				// Datatypes
				"Vector3",
				"Color3",
				"Matrix4",
				"Vector2"
		}) + ")\\b";
		
		final String LOGIC_PATTERN = "\\p{Punct}";
		final String NUMBER_PATTERN = "^\\d*\\.\\d+|\\d+\\.\\d*$";
		final String PAREN_PATTERN = "\\(" + "|" + "\\)";
		final String BRACKET_PATTERN = "\\[|\\]";
		final String STRING_PATTERN = "(\\[\\[)(.|\\R)*?(\\]\\])" + "|" + "\"([^\"\\\\]|\\\\.)*\"";
		final String COMMENT_PATTERN = "--(\\[\\[)(.|\\R)*?(\\]\\])" + "|" + "--[^\n]*";
		
		Pattern PATTERN = Pattern.compile(
						"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
					 + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					 + "|(?<STRING>" + STRING_PATTERN + ")"
					 + "|(?<LOGIC>" + LOGIC_PATTERN + ")"
					 + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
					 + "|(?<PAREN>" + PAREN_PATTERN + ")"
					 + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
				);
		
		// Do syntax highlighting
		code.setOnTextChange((event)->{
			code.resetHighlighting();

			String text = code.getText();
			Matcher matcher = PATTERN.matcher(text);
			
			// Iterate through pattern recognition, apply highlighting.
			while ( matcher.find() ) {
				int start = matcher.start();
				int end = matcher.end()-1;
				
				// Apply syntax
				if ( matcher.group("KEYWORD") != null ) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#2239a8")).style(FontStyle.BOLD));
				}
				
				if ( matcher.group("COMMENT") != null ) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#5f9ea0")));
				}
				
				if ( matcher.group("STRING") != null ) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#800080")));
				}
				
				if ( matcher.group("LOGIC") != null || matcher.group("PAREN") != null || matcher.group("BRACKET") != null ) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#7F7F00")));
				}
				
				if ( matcher.group("NUMBER") != null ) {
					code.setHighlighting(start, end,
							new FontMetaData().color(new Color("#2B957F")));
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
		
		if ( System.currentTimeMillis() - lastSave > 500 ) {
			try {
				inst.setSource(code.getText());
				lastSave = System.currentTimeMillis();
			}catch(Exception e) {
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
		if ( inst == null )
			return;
		inst.setSource(code.getText());
	}
}
