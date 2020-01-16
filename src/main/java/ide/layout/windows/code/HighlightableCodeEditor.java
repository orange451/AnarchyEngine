package ide.layout.windows.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lwjgui.font.Font;
import lwjgui.font.FontMetaData;
import lwjgui.font.FontStyle;
import lwjgui.paint.Color;
import lwjgui.scene.control.CodeArea;
import lwjgui.theme.Theme;

public abstract class HighlightableCodeEditor extends CodeArea {
	protected Pattern regexHighlightPattern;
	private List<HighlightData> cacheRegexData;
	
	public HighlightableCodeEditor() {
		this.setFontSize(16);
		this.setFont(Font.COURIER);
		this.setFillToParentHeight(true);
		this.setFillToParentWidth(true);
		
		// Do syntax highlighting on pattern
		this.setOnTextChange((event) -> {
			this.resetHighlighting();
			if (Theme.current() instanceof lwjgui.theme.ThemeWhite)
				this.setFontFill(Color.BLACK);

			this.applyHighlighting();
		});
		
		this.regeneratePattern();
	}
	
	protected void regeneratePattern() {
		this.regexHighlightPattern = generatePattern();
	}
	
	protected abstract Map<String, HighlightData> getRegexMap();
	
	protected Map<String, HighlightData> getFinalRegexMap() {
		return this.getRegexMap();
	}

	private Pattern generatePattern() {
		// Get highlight data as list
		Map<String, HighlightData> regexData = getFinalRegexMap();
		List<HighlightData> values = new ArrayList<>();
		Iterator<Entry<String, HighlightData>> iterator = regexData.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, HighlightData> entry = iterator.next();
			HighlightData val = entry.getValue();
			val.keyword(entry.getKey());
			
			if (val.regex == null || val.regex.length() == 0)
				continue;
			
			values.add(val);
		}
		
		// Sort highlight data
		Collections.sort(values, new Comparator<HighlightData>() {
			@Override
			public int compare(HighlightData o1, HighlightData o2) {
				return o2.priority-o1.priority;
			}
		});
		
		// Build overall regex string
		StringBuilder regexBuilder = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			HighlightData data = values.get(i);
			
			if ( i > 0 )
				regexBuilder.append("|");
			
			regexBuilder.append("(?<");
			regexBuilder.append(data.keyword);
			regexBuilder.append(">");
			regexBuilder.append(data.regex);
			regexBuilder.append(")");
		}
		
		cacheRegexData = values;
		String regex = regexBuilder.toString();
		//String SEARCH_PATTERN = searchTerm==null?"":searchTerm;
		//SEARCH_PATTERN = SEARCH_PATTERN.replaceAll("[\\W]", "\\\\$0");
		
		return Pattern.compile(regex);
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
			
			for (int i = 0; i < cacheRegexData.size(); i++) {
				HighlightData hData = cacheRegexData.get(i);
				if ( matcher.group(hData.keyword) != null ) {
					this.setHighlighting(start, end, hData.fontMeta);
				}
			}
		}
	}
	
	class HighlightData {
		private int priority = 0;
		private String regex;
		private FontMetaData fontMeta;
		private String keyword;
		
		private HighlightData keyword(String keyword) {
			this.keyword = keyword;
			return this;
		}
		
		public HighlightData priority(int priority) {
			this.priority = priority;
			return this;
		}

		public HighlightData fontMeta(FontMetaData fontMeta) {
			this.fontMeta = fontMeta;
			return this;
		}

		public HighlightData regex(String string) {
			this.regex = string;
			return this;
		}
		
		
	}
}
