package ide.layout.windows;

import engine.Game;
import engine.InternalRenderThread;
import engine.gl.IPipeline;
import engine.gl.Surface;
import engine.lua.type.object.services.UserInputService;
import ide.IDE;
import ide.layout.IdePane;
import lwjgui.event.ScrollEvent;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.gl.GenericShader;
import lwjgui.gl.Renderer;
import lwjgui.paint.Color;
import lwjgui.scene.Context;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.OpenGLPane;

public class IdeGameView extends IdePane {
	private OpenGLPane internal;
	private IPipeline pipeline;
	private Label fps;
	
	public IdeGameView(IPipeline pipeline) {
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
		internal.setOnMousePressed(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMousePress(event.button);
			cached_context.setSelected(internal);
		});
		internal.setOnMouseReleased(event -> {
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			uis.onMouseRelease(event.button);
		});
		internal.setOnMouseScrolled(event ->{
			UserInputService uis = (UserInputService) Game.getService("UserInputService");
			
			if ( !internal.isDescendentHovered() && !this.cached_context.isHovered(internal) )
				return;
			
			uis.onMouseScroll(((ScrollEvent)event).y > 0 ? 3 : 4 );
		});
		
		this.fps = new Label("fps");
		this.fps.setTextFill(Color.WHITE);
		this.fps.setMouseTransparent(true);
		this.internal.getChildren().add(fps);
		this.internal.setAlignment(Pos.TOP_LEFT);
		this.internal.setPadding(new Insets(2,2,2,2));
		
		StandardUserControls.bind(this);
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
		
		super.render(context);

		IDE.pipeline.setSize((int)internal.getWidth(), (int)internal.getHeight());
	}
}
