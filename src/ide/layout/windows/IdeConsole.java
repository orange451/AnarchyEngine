package ide.layout.windows;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.lwjgl.glfw.GLFW;

import engine.lua.LuaEngine;
import ide.layout.IdePane;
import lwjgui.LWJGUI;
import lwjgui.font.Font;
import lwjgui.geometry.Pos;
import lwjgui.scene.control.text_input.TextArea;
import lwjgui.scene.control.text_input.TextField;
import lwjgui.scene.layout.BorderPane;

public class IdeConsole extends IdePane {

	public IdeConsole() {
		super("Console", true);
		
		BorderPane editBox = new BorderPane();
		editBox.setAlignment(Pos.TOP_LEFT);
		this.getChildren().add(editBox);

		TextArea console = new TextArea();
		console.setFont(Font.COURIER);
		console.setEditable(false);
		console.setFillToParentHeight(true);
		console.setFillToParentWidth(true);
		editBox.setCenter(console);
		
		TextField luaInput = new TextField();
		luaInput.setPrompt("Lua Command Line");
		luaInput.setFillToParentWidth(true);
		editBox.setBottom(luaInput);

		this.setOnKeyPressed(event -> {
			if ( event.getKey() != GLFW.GLFW_KEY_ENTER )
				return;
			
			if ( cached_context.getSelected() == null )
				return;
			
			if (cached_context.getSelected().isDescendentOf(luaInput) ) {
				String lua = luaInput.getText();
				console.appendText("> " + lua+"\n");
				LuaEngine.runLua(lua);
				luaInput.setText("");
			}
		});
		
		// Print all text to console
		LuaEngine.globals.STDOUT = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = ""+(char)b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);
		
		// Print all errors to console
		LuaEngine.globals.STDERR = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = ""+(char)b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);

		
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}
}