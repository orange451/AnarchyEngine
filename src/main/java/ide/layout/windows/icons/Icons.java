package ide.layout.windows.icons;

import lwjgui.scene.image.Image;
import lwjgui.scene.image.ImageView;

public class Icons {
	public static final Icons icon_wat = new Icons("wat.gif");
	public static final Icons icon_cross = new Icons("Cross.png");
	public static final Icons icon_animation_data = new Icons("AnimationData.png");
	public static final Icons icon_world = new Icons("World.png");
	public static final Icons icon_folder = new Icons("Folder.png");
	public static final Icons icon_asset_folder = new Icons("AssetFolder.png");
	public static final Icons icon_cut = new Icons("Cut.png");
	public static final Icons icon_copy = new Icons("Copy.png");
	public static final Icons icon_paste = new Icons("Paste.png");
	public static final Icons icon_new = new Icons("New.png");
	public static final Icons icon_save = new Icons("Save.png");
	public static final Icons icon_saveas = new Icons("SaveAs.png");
	public static final Icons icon_script_service = new Icons("ScriptService.png");
	public static final Icons icon_script = new Icons("Script.png");
	public static final Icons icon_script_local = new Icons("ScriptLocal.png");
	public static final Icons icon_script_global = new Icons("ScriptGlobal.png");
	public static final Icons icon_player_scripts = new Icons("PlayerScripts.png");
	public static final Icons icon_workspace = new Icons("World.png");
	public static final Icons icon_storage = new Icons("Storage.png");
	public static final Icons icon_players = new Icons("Players.png");
	public static final Icons icon_player = new Icons("Player.png");
	public static final Icons icon_play = new Icons("Play.png");
	public static final Icons icon_play_server = new Icons("PlayServer.png");
	public static final Icons icon_camera = new Icons("Camera.png");
	public static final Icons icon_keyboard = new Icons("Keyboard.png");
	public static final Icons icon_network = new Icons("Network.png");
	public static final Icons icon_network_player = new Icons("NetworkPlayer.png");
	public static final Icons icon_network_server = new Icons("NetworkServer.png");
	public static final Icons icon_gameobject = new Icons("GameObject.png");
	public static final Icons icon_model = new Icons("Model.png");
	public static final Icons icon_box = new Icons("Box.png");
	public static final Icons icon_mesh = new Icons("Mesh.png");
	public static final Icons icon_texture = new Icons("Texture.png");
	public static final Icons icon_material = new Icons("Material.png");
	public static final Icons icon_light = new Icons("Light.png");
	public static final Icons icon_film = new Icons("Film.png");
	public static final Icons icon_film_timeline = new Icons("FilmTimeline.png");
	public static final Icons icon_skybox = new Icons("Skybox.png");
	
	private final Image image;
	
	public Icons(String path) {
		image = new Image("ide/layout/windows/icons/" + path);
	}
	
	public Image getImage() {
		return image;
	}
	
	public ImageView getView() {
		ImageView iconView = new ImageView(image);
		iconView.setPrefSize(16, 16);
		return iconView;
	}
}
