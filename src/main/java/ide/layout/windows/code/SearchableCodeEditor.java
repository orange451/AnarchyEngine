package ide.layout.windows.code;

import java.util.Map;
import org.lwjgl.glfw.GLFW;

import lwjgui.LWJGUI;
import lwjgui.font.Font;
import lwjgui.font.FontMetaData;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Group;
import lwjgui.scene.Node;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.Pane;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.floating.FloatingPane;
import lwjgui.style.BorderStyle;
import lwjgui.style.BoxShadow;
import lwjgui.theme.Theme;

public abstract class SearchableCodeEditor extends HighlightableCodeEditor {
	private SearchBar searchbar;
	private String searchTerm = "";
	
	public SearchableCodeEditor() {
		// Hacky way to inject search tracker
		Group searchTracker = new Group();
		searchTracker.setOnKeyPressed((event)->{
			if (event.isConsumed())
				return;
			
			if (SearchableCodeEditor.this.getParent().isDescendentSelected()) {
				if (event.getKey() == GLFW.GLFW_KEY_F && event.isCtrlDown) {
					System.out.println("Opening searchbar... " + this.searchbar);
					if (searchbar == null) {
						((Pane)SearchableCodeEditor.this.getParent()).getChildren().add(searchbar = new SearchBar());
					} else {
						if (this.searchbar.isSelected() || this.searchbar.isDescendentSelected()) {
							closeSearchbar();
						}
					}

					if (searchbar != null) {
						if (getSelectedText().length() > 0) {
							searchbar.input.setText(getSelectedText());
							setSearchTerm(getSelectedText());
						}

						this.window.getContext().setSelected(this.searchbar.input);
						this.searchbar.input.selectAll();
					}
				}
			}
		});
		this.children.add(searchTracker);
	}
	
	private void setSearchTerm(String term) {
		this.searchTerm = term;
		this.regeneratePattern();
		this.applyHighlighting();
	}
	
	@Override
	protected Map<String, HighlightData> getFinalRegexMap() {
		Map<String, HighlightData> ret = super.getFinalRegexMap();
		
		String SEARCH_PATTERN = searchTerm==null?"":searchTerm;
		SEARCH_PATTERN = SEARCH_PATTERN.replaceAll("[\\W]", "\\\\$0");
		
		ret.put("SEARCH", new HighlightData()
							.priority(Integer.MIN_VALUE)
							.regex(SEARCH_PATTERN)
							.fontMeta(new FontMetaData()
									.background(Color.YELLOW.alpha(0.7f))
									.color(Color.BLACK)));
		
		return ret;
	}
	
	private void closeSearchbar() {
		setSearchTerm("");
		((Pane)SearchableCodeEditor.this.getParent()).getChildren().remove(searchbar);
		searchbar = null;
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
				closeSearchbar();
			});

			// Force our input to be selected
			window.getContext().setSelected(input);
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
}
