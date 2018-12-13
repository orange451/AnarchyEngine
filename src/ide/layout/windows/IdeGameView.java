package ide.layout.windows;

import engine.Game;
import engine.InternalRenderThread;
import engine.gl.Pipeline;
import engine.gl.Surface;
import ide.IDE;
import ide.layout.IdePane;
import luaengine.type.object.services.UserInputService;
import lwjgui.Color;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.gl.GenericShader;
import lwjgui.gl.Renderer;
import lwjgui.scene.Context;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.OpenGLPane;

public class IdeGameView extends IdePane {
	private OpenGLPane internal;
	private Pipeline pipeline;
	private Label fps;
	
	public IdeGameView(Pipeline pipeline) {
		super("Game", false);
		
		this.pipeline = pipeline;
		this.setAlignment(Pos.CENTER);
		
		internal = new OpenGLPane();
		internal.setMinSize(1, 1);
		internal.setFillToParentHeight(true);
		internal.setFillToParentWidth(true);
		internal.setFlipY(true);
		internal.setRendererCallback(new Renderer() {
			GenericShader shader;
			{
				shader = new GenericShader();
			}
			
			@Override
			public void render(Context context) {
				Surface surface = IdeGameView.this.pipeline.getPipelineBuffer();
				surface.render(shader);
			}
		});
		this.getChildren().add(internal);
		
		internal.setOnKeyPressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			
			if ( !internal.isDescendentSelected() )
				return;
			
			uis.onKeyPressed(event.getKey());
		});
		internal.setOnKeyReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			
			if ( !internal.isDescendentSelected() )
				return;
			
			uis.onKeyReleased(event.getKey());
		});
		internal.setMousePressedEvent(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMousePress(event.button);
			cached_context.setSelected(internal);
		});
		internal.setMouseReleasedEvent(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMouseRelease(event.button);
		});
		
		this.fps = new Label("fps");
		this.fps.setTextFill(Color.WHITE);
		this.fps.setMouseTransparent(true);
		this.internal.getChildren().add(fps);
		this.internal.setAlignment(Pos.TOP_LEFT);
		this.internal.setPadding(new Insets(2,2,2,2));
	}

	@Override
	public void onOpen() {
		IDE.pipeline.setEnabled(true);
	}

	@Override
	public void onClose() {
		IDE.pipeline.setEnabled(false);
	}
	
	@Override
	public void render(Context context) {
		fps.setText(InternalRenderThread.fps + " fps");
		IDE.pipeline.setSize((int)internal.getWidth(), (int)internal.getHeight());
		
		super.render(context);
	}
}
