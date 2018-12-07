package ide.layout.windows;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.Game;
import engine.GameSubscriber;
import ide.IDEFilePath;
import ide.layout.IdePane;
import luaengine.type.LuaValuetype;
import luaengine.type.object.Instance;
import luaengine.type.object.InstancePropertySubscriber;
import lwjgui.Color;
import lwjgui.LWJGUI;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.scene.Node;
import lwjgui.scene.Region;
import lwjgui.scene.control.CheckBox;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.ScrollPane.ScrollBarPolicy;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.ColumnConstraint;
import lwjgui.scene.layout.FontStyle;
import lwjgui.scene.layout.GridPane;
import lwjgui.scene.layout.Priority;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.theme.Theme;

public class IdeProperties extends IdePane implements GameSubscriber,InstancePropertySubscriber {
	private ScrollPane scroller;
	private Instance currentInstance;
	
	private PropertyGrid grid;
	
	static Color alt = Theme.currentTheme().getControlAlt();
	
	public IdeProperties() {
		super("Properties", true);
		
		this.scroller = new ScrollPane();
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.scroller.setHbarPolicy(ScrollBarPolicy.NEVER);
		this.getChildren().add(scroller);
		
		
		Game.getGame().subscribe(this);
		
		this.grid = new PropertyGrid();
		this.scroller.setContent(grid);
		
		update();
	}

	public void update() {
		if ( !Game.isLoaded() )
			return;
		
		List<Instance> selected = Game.selected();
		
		LWJGUI.runLater(() -> {
			clear();
			if ( selected.size() != 1 )
				return;
			
			if ( grid.inst != null && currentInstance != null && currentInstance.equals(grid.inst) )
				return;
			
			currentInstance = selected.get(0);
			currentInstance.attachPropertySubscriber(this);
			
			grid.setInstance(currentInstance);
		});
	}
	
	private void clear() {
		grid.clear();
		if ( currentInstance != null )
			currentInstance.detach(this);
		currentInstance = null;	
	}
	
	@Override
	public void onPropertyChange(Instance instance, String property, LuaValue value) {
		LWJGUI.runLater(()-> {
			grid.update(instance, property, value);
		});
	}
	
	private static PropertyModifier getPropertyModifier( Instance instance, String field, LuaValue value ) {
		// Calculate editable
		boolean editable = instance.getField(field).canModify();
		if ( (field.equals("Name") || field.equals("Parent")) && instance.isLocked() )
			editable = false;
		
		// Edit a filepath
		if ( field.equals("FilePath") )
			return new FilePathPropertyModifier(instance, field, value, editable);
		
		// Edit a boolean
		if ( value.isboolean() )
			return new BooleanPropertyModifier(instance, field, value, editable);
		
		// Edit a string/number
		if ( value.isstring() || value.isnumber() )
			return new StringPropertyModifier(instance, field, value, editable);
		
		// Edit a datatype
		if ( value instanceof LuaValuetype )
			return new DataTypePropertyModifier(instance, field, value, editable);
		
		// Edit an instance pointer
		if ( instance.getField(field).getType().equals(Instance.class) )
			return new InstancePropertyModifier(instance, field, value, editable);
		
		// Fallback
		return new PropertyModifierTemp( instance, field, value, editable );
	}
	
	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}

	@Override
	public void gameUpdateEvent(boolean important) {
		update();
	}
	
	static class PropertyModifier extends StackPane {
		protected Instance instance;
		protected String field;
		protected LuaValue initialValue;
		protected boolean editable;
		
		public PropertyModifier( Instance instance, String field, LuaValue initialValue, boolean editable ) {
			this.instance = instance;
			this.field = field;
			this.initialValue = initialValue;
			this.editable = editable;
			
			this.setPadding(new Insets(0,0,0,4));
			this.setPrefSize(1, 1);
		}
	}
	
	static class PropertyModifierTemp extends PropertyModifier {
		protected Label label;
		
		public PropertyModifierTemp(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue,editable);
			
			label = new Label(initialValue.toString());
			label.setFontSize(16);
			label.setMouseTransparent(true);
			this.getChildren().add(label);
			
			if ( !editable )
				label.setTextFill(Color.GRAY);
		}
	}
	
	static class BooleanPropertyModifier extends PropertyModifier {
		private CheckBox check;
		
		public BooleanPropertyModifier(Instance instance, String field, LuaValue value, boolean editable) {
			super(instance,field,value,editable);
			
			this.check = new CheckBox(value.toString());
			this.check.setFontSize(16);
			this.check.setSize(16);
			this.check.setChecked(value.checkboolean());
			this.getChildren().add(check);
			
			if ( editable ) {
				this.check.setOnAction(event -> {
					this.check.setChecked(!this.check.isChecked());
					this.instance.set(field, LuaValue.valueOf(this.check.isChecked()));
					this.check.setText(""+this.check.isChecked());
				});
			}
		}
	}
	
	static abstract class PropertyModifierInput extends PropertyModifierTemp {
		private TextField textField;
		private boolean editing;
		
		public PropertyModifierInput(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);

			this.setPadding(Insets.EMPTY);
			this.textField = new TextField() {
				{
					((Region)this.getChildren().get(0)).setPadding(new Insets(0,0,0,4));
				}
			};
			this.textField.setPreferredColumnCount(1024);
			this.setMaxHeight(16);
			this.textField.setAlignment(Pos.CENTER_LEFT);
			
			if ( editable ) {
				this.setMouseReleasedEvent(event -> {
					textField.setText(label.getText());
					this.getChildren().add(textField);
					editing = true;

					LWJGUI.runLater(()-> {
						cached_context.setSelected(textField);
						textField.selectAll();
					});
				});
			}
			
			this.setOnKeyPressed(event -> {
				int key = event.getKey();
				
				if ( key == GLFW.GLFW_KEY_ENTER ) {
					if ( !editing )
						return;
					
					cancel();
					try {
						onValueSet(textField.getText());
						label.setText(instance.get(field).toString());
					} catch(Exception e) {
						//e.printStackTrace();
					}
				}
				
				if ( key == GLFW.GLFW_KEY_ESCAPE ) {
					cancel();
				}
			});
		}
		
		public abstract void onValueSet(String text);
		
		private void cancel() {
			if ( !this.getChildren().contains(textField) )
				return;
			
			editing = false;
			this.getChildren().remove(textField);
		}
		
		@Override
		public void position(Node parent) {
			super.position(parent);
			
			if ( !this.isDescendentSelected() ) {
				cancel();
			}
			
			label.offset(4, 0);
		}
		
	}
	
	static class StringPropertyModifier extends PropertyModifierInput {
		public StringPropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
		}

		@Override
		public void onValueSet(String text) {
			instance.set(field, LuaValue.valueOf(text));
		}
	}
	
	static class DataTypePropertyModifier extends PropertyModifierInput {
		public DataTypePropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
		}

		@Override
		public void onValueSet(String text) {
			instance.set(field, ((LuaValuetype)initialValue).clone().fromString(text));
		}
	}
	
	static class FilePathPropertyModifier extends PropertyModifierTemp {
		public FilePathPropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
			
			if ( editable ) {
				this.setMouseReleasedEvent(event -> {
					
					try {
						PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
						String pp = IDEFilePath.convertToSystem(initialValue.toString());
						if ( !new File(pp).exists() )
							pp = "";
						int result = NativeFileDialog.NFD_OpenDialog(null, pp, outPath);
						if ( result == NativeFileDialog.NFD_OKAY ) {
							String path = IDEFilePath.convertToIDE(outPath.getStringUTF8(0));
							try {
								this.instance.set(field, path);
							}catch(Exception e) {
								e.printStackTrace();
							}
						} else {
							//
						}
					}catch(Exception e1) {
						e1.printStackTrace();
					}
				});
			}
		}
	}
	
	static class InstancePropertyModifier extends PropertyModifierTemp {
		private boolean selected;
		private StackPane t;
		
		public InstancePropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
			
			if ( editable ) {
				this.setMouseReleasedEvent(event -> {
					if ( !selected ) {
						selected = true;
						t = new StackPane();
						t.setPrefSize(this.getWidth(),this.getHeight());
						t.setBackground(new Color(0, 32, 255, 128));
						this.getChildren().add(t);
					} else {
						cancel();
					}
				});
				
				this.setOnKeyPressed(event -> {
					if ( !selected )
						return;
					
					if ( event.getKey() == GLFW.GLFW_KEY_ESCAPE )
						cancel();
				});
			}
		}
		
		@Override
		public void position(Node node) {
			super.position(node);
			
			if ( !selected )
				return;
			
			// We clicked /something/
			Node n = this.cached_context.getSelected();
			if ( !n.isDescendentOf(this) ) {
				List<Instance> temp = Game.selected();
				if ( temp.size() != 1 ) {
					cancel();
					return;
				}
				
				Instance t = temp.get(0);
				if ( t.equals(instance) ) {
					cancel();
					return;
				}
				
				set(t);
			}
		}
		
		public void set(LuaValue instance) {
			if ( instance == null || (!instance.isnil() && !(instance instanceof Instance)) )
				return;
			
			this.cancel();
			try {
				this.instance.set(field, instance);
			}catch(Exception e) {
				//
			}
			
			Game.deselectAll();
			Game.select(this.instance);
		}
		
		public void cancel() {
			selected = false;
			
			if ( t == null )
				return;
			this.getChildren().remove(t);
			t = null;
		}
	}
	
	static class PropertyGrid extends VBox {
		private Label l;
		private GridPane internal;
		private Instance inst;
		
		private HashMap<String,PropertyModifier> props = new HashMap<String,PropertyModifier>();
		
		public PropertyGrid() {
			// Create main underneath pane
			// It's gray so that the spacing in the grid cells look like there's an outline.
			this.setFillToParentWidth(true);
			this.setPrefSize(0, 0);
			this.setAlignment(Pos.TOP_LEFT);
			
			// Create title bar
			StackPane top = new StackPane();
			top.setFillToParentWidth(true);
			top.setPadding(new Insets(2,2,2,2));
			top.setBackground(Color.DIM_GRAY);
			top.setAlignment(Pos.CENTER_LEFT);
			top.setPrefSize(1, 1);
			this.getChildren().add(top);
			this.l = new Label("");
			l.setFontStyle(FontStyle.BOLD);
			l.setFontSize(16);
			top.getChildren().add(l);
	
			// Create column constraints
			ColumnConstraint const1 = new ColumnConstraint( 100 );
			const1.setFillWidth(true);
			ColumnConstraint const2 = new ColumnConstraint( 0 );
			const2.setHgrow(Priority.ALWAYS);
			const2.setFillWidth(true);
			
			// Create grid
			internal = new GridPane();
			internal.setBackground(Color.LIGHT_GRAY);
			internal.setFillToParentWidth(true);
			internal.setHgap(1);
			internal.setVgap(1);
			internal.setColumnConstraint(0, const1);
			internal.setColumnConstraint(1, const2);

			this.getChildren().add(internal);
		}
		
		private void fill() {
			// Fill grid
			String[] fields = inst.getFieldsOrdered();
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i];
				LuaValue value = inst.get(field);
				
				Label fieldLabel = new Label(field);
				fieldLabel.setFontSize(16);
				
				// Cell 1
				StackPane t1 = new StackPane();
				if ( i % 2 == 1 )
					t1.setBackground(alt);
				t1.setPadding(new Insets(0,0,0,12));
				t1.setPrefSize(1, 1);
				t1.getChildren().add(fieldLabel);
				
				// Cell 2
				PropertyModifier t2 = getPropertyModifier( inst, field, value );
				if ( i % 2 == 1 )
					t2.setBackground(alt);
				if ( t2 instanceof PropertyModifierTemp ) {
					fieldLabel.setTextFill(((PropertyModifierTemp) t2).label.getTextFill());
				}
				
				// Add them to grid
				getInternal().add(t1, 0, i);
				getInternal().add(t2, 1, i);
				props.put(field, t2);
			}
		}

		public void update(Instance instance, String property, LuaValue value) {
			if ( inst != null && !instance.equals(inst))
				return;
			
			if ( instance.getFields().length != props.size() ) {
				setInstance(instance);
				return;
			}
			
			PropertyModifier p = props.get(property);
			if ( p == null ) {
				return;
			}
			
			PropertyModifierTemp p2 = null;
			if ( p instanceof PropertyModifierTemp )
				p2 = (PropertyModifierTemp)p;
			
			if ( p2 == null )
				return;
			
			p2.label.setText(value.toString());
		}

		public void clear() {
			props.clear();
			this.inst = null;
			internal.clear();
			l.setText("");
		}

		public GridPane getInternal() {
			return this.internal;
		}

		public void setInstance(Instance inst) {
			clear();
			
			this.inst = inst;
			l.setText(inst.getFullName());
			
			fill();
		}
	}
}
