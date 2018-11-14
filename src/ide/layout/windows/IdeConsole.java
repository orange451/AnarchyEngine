package ide.layout.windows;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.lwjgl.glfw.GLFW;

import ide.layout.IdePane;
import luaengine.LuaEngine;
import lwjgui.LWJGUI;
import lwjgui.geometry.Pos;
import lwjgui.scene.control.TextArea;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.BorderPane;

public class IdeConsole extends IdePane {

	public IdeConsole() {
		super("Console", true);
		
		BorderPane editBox = new BorderPane();
		editBox.setAlignment(Pos.TOP_LEFT);
		this.getChildren().add(editBox);

		TextArea console = new TextArea();
		console.setEditable(false);
		console.setPreferredColumnCount(1024);
		console.setPreferredRowCount(1024);
		editBox.setCenter(console);
		
		TextField luaInput = new TextField();
		luaInput.setPrompt("Lua Command Line");
		luaInput.setPreferredColumnCount(1024);
		editBox.setBottom(luaInput);

		this.setOnKeyPressed(event -> {
			if ( event.getKey() != GLFW.GLFW_KEY_ENTER )
				return;
			
			if ( cached_context.getSelected() == null )
				return;
			
			if (cached_context.getSelected().isDescendentOf(luaInput) ) {
				String lua = luaInput.getText();
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