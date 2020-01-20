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

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.luaj.vm2.LuaValue;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NativeFileDialog;

import engine.FilePath;
import engine.Game;
import engine.GameSubscriber;
import engine.InternalRenderThread;
import engine.lua.LuaEngine;
import engine.lua.lib.LuaTableReadOnly;
import engine.lua.type.Clamp;
import engine.lua.type.LuaField;
import engine.lua.type.LuaValuetype;
import engine.lua.type.NumberClamp;
import engine.lua.type.NumberClampPreferred;
import engine.lua.type.data.Color3;
import engine.lua.type.data.Color4;
import engine.lua.type.data.ColorBase;
import engine.lua.type.object.AssetLoadable;
import engine.lua.type.object.Instance;
import engine.lua.type.object.InstancePropertySubscriber;
import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.ui.CSS;
import engine.tasks.TaskManager;
import ide.layout.IdePane;
import lwjgui.LWJGUI;
import lwjgui.font.FontStyle;
import lwjgui.geometry.Insets;
import lwjgui.geometry.Pos;
import lwjgui.paint.Color;
import lwjgui.scene.Node;
import lwjgui.scene.control.Button;
import lwjgui.scene.control.CheckBox;
import lwjgui.scene.control.ColorPicker;
import lwjgui.scene.control.ComboBox;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.Label;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.ScrollPane;
import lwjgui.scene.control.ScrollPane.ScrollBarPolicy;
import lwjgui.scene.control.Slider;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.ColumnConstraint;
import lwjgui.scene.layout.GridPane;
import lwjgui.scene.layout.HBox;
import lwjgui.scene.layout.Priority;
import lwjgui.scene.layout.StackPane;
import lwjgui.scene.layout.VBox;
import lwjgui.theme.Theme;

public class IdeProperties extends IdePane implements GameSubscriber,InstancePropertySubscriber {
	private ScrollPane scroller;
	private Instance currentInstance;
	
	private PropertyGrid grid;
	
	public IdeProperties() {
		super("Properties", true);
		
		this.scroller = new ScrollPane();
		this.scroller.setBorder(Insets.EMPTY);
		this.scroller.setFillToParentHeight(true);
		this.scroller.setFillToParentWidth(true);
		this.scroller.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
		this.getChildren().add(scroller);
		
		
		Game.getGame().subscribe(this);
		
		this.grid = new PropertyGrid();
		this.scroller.setContent(grid);
		
		update(true);
		
		AtomicLong last = new AtomicLong();
		Game.runService().renderSteppedEvent().connect((args)->{
			long now = System.currentTimeMillis();
			long then = last.get();
			long elapsed = now-then;
			
			if ( elapsed > 20 ) {
				if ( requiresUpdate ) {
					update(true);
				}
				last.set(System.currentTimeMillis());
			}
		});
		
		// Add user controls
		StandardUserControls.bind(this);
	}
	
	private long lastUpdate = -1;
	private boolean requiresUpdate; // TODO Maybe check in game logic thread if this is true and then force update if lastUpdate hasn't updated in more than 5 ms?

	private void update(boolean important) {
		if ( !Game.isLoaded() )
			return;

		if (System.currentTimeMillis()-lastUpdate < 50 && !important ) {
			requiresUpdate = true;
			return;
		}
		
		lastUpdate = System.currentTimeMillis();
		requiresUpdate = false;
		
		List<Instance> selected = Game.selected();
		
		LWJGUI.runLater(() -> {
			if ( selected.size() != 1 )
				return;
			
			Instance temp = selected.get(0);
			if ( temp == grid.inst )
				return;
			
			clear();
			currentInstance = temp;
			currentInstance.attachPropertySubscriber(this);
			grid.setInstance(currentInstance);
			grid.updateChildren();
			grid.render(window.getContext());
		});
	}
	
	private void clear() {
		grid.clear();
		if ( currentInstance != null )
			currentInstance.detach(this);
		currentInstance = null;	
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
		update(false);
	}
	
	@Override
	public void onPropertyChange(Instance instance, LuaValue property, LuaValue value) {
		//System.out.println(instance.getFullName() + " :: " + property + " => " + value);
		
		LWJGUI.runLater(()-> {
			grid.update(instance, property, value);
		});
	}
	
	private static PropertyModifier getPropertyModifier( Instance instance, String field, LuaValue value ) {
		LuaField luaField = instance.getField(LuaValue.valueOf(field));
		
		if ( luaField == null )
			return null;
		
		// Calculate editable
		boolean editable = luaField.canModify();
		if ( (field.equals("Name") || field.equals("Parent")) && instance.isLocked() )
			editable = false;
		
		// Display "Linked Source" instead of the actual source
		if ( instance instanceof ScriptBase && field.equals("Source") )
			return new StringPropertyModifier(instance, field, LuaValue.valueOf("[Linked Source]"), false);
		
		// Display "Linked Source" instead of the actual source
		if ( instance instanceof CSS && field.equals("Source") )
			return new StringPropertyModifier(instance, field, LuaValue.valueOf("[Linked Source]"), false);
		
		// Enum modifier
		if ( luaField.getEnumType() != null )
			return new EnumPropertyModifier(instance, field, value, editable);
		
		// Edit a filepath
		if ( field.equals("FilePath") )
			return new FilePathPropertyModifier(instance, field, value, editable);

		// Edit a color3
		if ( value instanceof Color3 )
			return new ColorPropertyModifier(instance, field, value, editable, false);

		// Edit a color4
		if ( value instanceof Color4 )
			return new ColorPropertyModifier(instance, field, value, editable, true);
		
		// Edit a boolean
		if ( value.isboolean() )
			return new BooleanPropertyModifier(instance, field, value, editable);
		
		// Clampable number value, use slider!
		Clamp<?> clamp = luaField.getClamp();
		if ( value.isnumber() && clamp != null && clamp instanceof NumberClamp )
			return new SliderPropertyModifier(instance, field, value, editable);
		
		// Edit a string/number
		if ( value.isstring() || value.isnumber() )
			return new StringPropertyModifier(instance, field, value, editable);
		
		// Edit a datatype
		if ( value instanceof LuaValuetype )
			return new DataTypePropertyModifier(instance, field, value, editable);
		
		// Edit an instance pointer
		if ( luaField.getType().equals(Instance.class) )
			return new InstancePropertyModifier(instance, field, value, editable);
		
		// Fallback
		return new PropertyModifierTemp( instance, field, value, editable );
	}
	
	public static void setWithHistory(Instance instance, LuaValue field, LuaValue newValue) {
		LuaValue oldValue = instance.get(field);
		instance.set(field, newValue);
		LuaValue newNewValue = instance.get(field);
		
		// Value must have actually changed in order to write to history
		if ( newNewValue.equals(oldValue) )
			return;
		
		Game.historyService().pushChange(instance, field, oldValue, newNewValue);
	}
	
	static abstract class PropertyModifier extends StackPane {
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
			this.setMaxHeight(20);
			this.setMinHeight(20);
			this.setAlignment(Pos.CENTER_LEFT);
			this.setFillToParentWidth(true);
		}
		
		public abstract void update(LuaValue value);
	}
	
	static class PropertyModifierTemp extends PropertyModifier {
		protected Label label;
		
		public PropertyModifierTemp(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue,editable);
			
			label = new Label(initialValue.toString());
			label.setFontSize(16);
			label.setMouseTransparent(true);
			this.setAlignment(Pos.CENTER_LEFT);
			this.getChildren().add(label);
			
			if ( !editable )
				label.setTextFill(Color.GRAY);
		}

		@Override
		public void update(LuaValue value) {
			label.setText(value.toString());
		}
	}
	
	static class BooleanPropertyModifier extends PropertyModifier {
		private CheckBox check;
		
		public BooleanPropertyModifier(Instance instance, String field, LuaValue value, boolean editable) {
			super(instance,field,value,editable);
			
			this.check = new CheckBox(value.toString());
			this.check.setFontSize(16);
			this.check.setSize(18);
			this.check.setChecked(value.checkboolean());
			this.check.setDisabled(!editable);
			this.getChildren().add(check);
			
			if ( editable ) {
				this.check.setOnAction(event -> {
					setWithHistory(this.instance, LuaValue.valueOf(field), LuaValue.valueOf(this.check.isChecked()));
					update(this.instance.get(field));
				});
			}
		}

		@Override
		public void update(LuaValue value) {
			check.setChecked(value.toboolean());
			this.check.setText(""+this.check.isChecked());
		}
	}
	
	static class SliderPropertyModifier extends PropertyModifier {
		private HBox hbox;
		private Slider slider;
		private PropertyModifierInput direct;
		
		public SliderPropertyModifier(Instance instance, String field, LuaValue value, boolean editable) {
			super(instance,field,value,editable);
			
			Clamp<?> clamp = instance.getField(LuaValue.valueOf(field)).getClamp();
			NumberClamp nc = (NumberClamp)clamp;
			
			float min = nc.getMin();
			float max = nc.getMax();
			if ( nc instanceof NumberClampPreferred ) {
				min = ((NumberClampPreferred)nc).getPreferredMin();
				max = ((NumberClampPreferred)nc).getPreferredMax();
			}
			
			this.hbox = new HBox();
			this.hbox.setAlignment(Pos.CENTER_LEFT);
			this.hbox.setBackground(null);
			this.getChildren().add(hbox);
			
			this.slider = new Slider(min, max, value.tofloat());
			this.slider.setDisabled(!editable);
			this.slider.setPrefWidth(100);
			this.hbox.getChildren().add(slider);
			
			this.direct = new StringPropertyModifier(instance, field, value, editable) {
				@Override
				public void onValueSet(String text) {
					super.onValueSet(text);
					slider.setValue(Double.parseDouble(text));
				}
			};
			this.direct.setBackground(null);
			this.direct.textField.setPrefWidth(32);
			this.direct.textField.setMinWidth(32);
			this.direct.textField.setMaxWidth(32);
			this.direct.textField.setFillToParentWidth(false);
			this.direct.setPrefWidth(32);
			this.hbox.getChildren().add(direct);
			
			if ( editable ) {
				this.slider.setOnValueChangedEvent(event->{
					double v = Math.floor(slider.getValue()*100)/100f;
					this.instance.set(field, slider.getValue());
					this.direct.label.setText(""+v);
				});
			}
		}
		
		public void update(LuaValue value) {
			slider.setValue(value.todouble());
			this.direct.label.setText(""+slider.getValue());
		}
	}
	
	static class ColorPropertyModifier extends PropertyModifier {
		private ColorPicker picker;
		
		public ColorPropertyModifier(Instance instance, String field, LuaValue value, boolean editable, boolean supportsAlpha) {
			super(instance,field,value,editable);
			
			Color color = Color.WHITE;
			if ( value instanceof Color3 )
				color = ((Color3)value).toColor();
			
			this.picker = new ColorPicker(color);
			this.picker.setMaxHeight(16);
			this.picker.setPadding(new Insets(0,8,0,8));
			this.picker.setFontSize(16);
			this.picker.setSupportsAlpha(supportsAlpha);
			this.getChildren().add(picker);
			
			if ( editable ) {
				this.picker.setOnAction(event -> {
					LuaValue output = supportsAlpha?new Color4(this.picker.getColor()):new Color3(this.picker.getColor());
					setWithHistory(this.instance, LuaValue.valueOf(field), output);
				});
			}
		}
		
		@Override
		public void update(LuaValue value) {
			this.picker.setColor(((ColorBase)value).toColor());
		}
	}
	
	private final static LuaValue C_ENUM = LuaValue.valueOf("Enum");
	
	static class EnumPropertyModifier extends PropertyModifier {
		public EnumPropertyModifier(Instance instance, String field, LuaValue value, boolean editable) {
			super(instance, field, value, editable);
			
			this.dropdown = new ComboBox<String>() {
				@Override
				public void resize() {
					super.resize();
					for (int i = 0; i < this.buttons.size(); i++) {
						Button button = this.buttons.get(i);
						button.setPadding(new Insets(0,8,0,8));
						button.setMaxHeight(20);
					}
				}
			};
			String enumName = instance.getField(LuaValue.valueOf(field)).getEnumType().getType();
			LuaTableReadOnly enm = (LuaTableReadOnly) LuaEngine.globals.get(C_ENUM).rawget(enumName);
			LuaValue[] keys = enm.keys();
			for (int i = 0; i < keys.length; i++) {
				String type = keys[i].toString();
				dropdown.getItems().add(type);
			}
			this.setPadding(Insets.EMPTY);
			this.dropdown.setMaxHeight(20);
			this.dropdown.setPrefWidth(100);
			this.dropdown.setValue(instance.get(field).toString());
			this.dropdown.setDisabled(!editable);
			this.getChildren().add(dropdown);
			
			if ( editable ) {
				this.dropdown.setOnAction((event)->{
					setWithHistory(this.instance, LuaValue.valueOf(field), LuaValue.valueOf(this.dropdown.getValue()));
				});
			}
		}

		private ComboBox<String> dropdown;

		@Override
		public void update(LuaValue value) {
			this.dropdown.setValue(value.toString());
		}
	}
	
	static abstract class PropertyModifierInput extends PropertyModifierTemp {
		protected TextField textField;
		protected boolean editing;
		
		public PropertyModifierInput(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);

			AtomicBoolean editingAtomic = new AtomicBoolean(false);
			
			this.setPadding(Insets.EMPTY);
			this.textField = new TextField() {
				{
					((ScrollPane)this.getChildren().get(0)).setInternalPadding(new Insets(0,0,0,3));
				}
				
				@Override
				protected void resize() {
					super.resize();
					
					if ( editing )  {
						editingAtomic.set(true);
					} else {
						if ( editingAtomic.get() ) {
							editingAtomic.set(false);
							try {
								onValueSet(textField.getText());
								label.setText(instance.get(field).toString());
							} catch(Exception e) {
								//e.printStackTrace();
							}
						}
					}
				}
			};
			//this.textField.setPreferredColumnCount(1024);
			//this.textField.setFillToParentWidth(true);
			this.textField.setAlignment(Pos.CENTER_LEFT);
			this.textField.setFillToParentWidth(true);
			
			if ( editable ) {
				this.setOnMouseReleased(event -> {
					textField.setText(label.getText());
					this.getChildren().add(textField);
					editing = true;
					window.getContext().setSelected(textField);
					textField.selectAll();
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
			this.textField.setPrefWidth(0);
			super.position(parent);
			this.textField.forceWidth(this.getWidth()-this.getPadding().getWidth());
			
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
			setWithHistory(this.instance, LuaValue.valueOf(field), LuaValue.valueOf(text));
		}
	}
	
	static class DataTypePropertyModifier extends PropertyModifierInput {
		public DataTypePropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
		}

		@Override
		public void onValueSet(String text) {
			LuaValuetype initial = (LuaValuetype)initialValue;
			LuaValuetype initialClone = initial.clone();
			LuaValuetype t = initialClone.fromString(text);
			setWithHistory(this.instance, LuaValue.valueOf(field), t);
		}
	}
	
	static class FilePathPropertyModifier extends PropertyModifierTemp {
		public FilePathPropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
			
			if ( editable ) {
				this.setOnMouseReleased(event -> {
					
					try {
						PointerBuffer outPath = MemoryUtil.memAllocPointer(1);
						String pp = FilePath.convertToSystem(initialValue.toString());
						if ( !new File(pp).exists() )
							pp = "";
						
						// Check if this object has defined file types
						String types = null;
						if ( instance instanceof AssetLoadable ) {
							Class<?> clazz = ((AssetLoadable)instance).getClass();
							
							Method m = clazz.getMethod("getFileTypes");
							types = (String) m.invoke(null);
						}
						
						// Open the dialogue to pick new path.
						int result = NativeFileDialog.NFD_OpenDialog(types, pp, outPath);
						if ( result == NativeFileDialog.NFD_OKAY ) {
							String path = FilePath.convertToIDE(outPath.getStringUTF8(0));
							try {
								setWithHistory(this.instance, LuaValue.valueOf(field), LuaValue.valueOf(path));
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
	
	private static boolean SELECTING_OBJECT;
	
	static class InstancePropertyModifier extends PropertyModifierTemp {
		private boolean selected;
		private StackPane t;
		
		public InstancePropertyModifier(Instance instance, String field, LuaValue initialValue, boolean editable) {
			super(instance, field, initialValue, editable);
			
			if ( editable ) {
				this.setOnMouseReleased(event -> {
					if ( !selected ) {
						selected = true;
						SELECTING_OBJECT = true;
						t = new StackPane();
						t.setAlignment(Pos.CENTER_RIGHT);
						t.setPrefSize(this.getWidth(),this.getHeight());
						t.setBackgroundLegacy(new Color(0, 32, 255, 128));
						this.getChildren().add(t);
						
						StackPane cancel = new StackPane();
						cancel.setBackgroundLegacy(new Color(200, 32, 32));
						cancel.setPrefSize(this.getHeight(), this.getHeight());
						cancel.setAlignment(Pos.CENTER);
						Label l = new Label("x");
						l.setMouseTransparent(true);
						cancel.getChildren().add(l);
						t.getChildren().add(cancel);
						
						cancel.setOnMouseClicked((event2)->{
							set(LuaValue.NIL);
						});
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
			Node n = this.window.getContext().getSelected();
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
			
			// Stop selection
			this.cancel();
			
			// Update selection
			try {
				setWithHistory(this.instance, LuaValue.valueOf(field), instance);
			}catch(Exception e) {
				//
			}
			
			// Handle game selection (since we clicked an object)
			Game.deselectAll();
			if ( instance != null && !instance.isnil() && instance instanceof Instance )
				Game.deselect((Instance) instance);
			Game.select(this.instance);
		}
		
		public void cancel() {
			selected = false;
			InternalRenderThread.runLater(()->{
				SELECTING_OBJECT = false;
			});
			
			if ( t == null )
				return;
			this.getChildren().remove(t);
			t = null;
		}
	}
	
	class PropertyGrid extends VBox {
		private Label l;
		private GridPane internal;
		private Instance inst;
		
		private HashMap<LuaValue,PropertyModifier> props = new HashMap<LuaValue,PropertyModifier>();
		
		public PropertyGrid() {
			// Create main underneath pane
			// It's gray so that the spacing in the grid cells look like there's an outline.
			this.setPrefSize(0, 0);
			this.setAlignment(Pos.TOP_LEFT);
			
			// Create title bar
			StackPane top = new StackPane();
			top.setFillToParentWidth(true);
			top.setPadding(new Insets(2,2,2,2));
			top.setBackgroundLegacy(Theme.current().getControlOutline());
			top.setAlignment(Pos.CENTER_LEFT);
			top.setPrefSize(1, 1);
			this.getChildren().add(top);
			this.l = new Label("");
			l.setFontStyle(FontStyle.BOLD);
			l.setFontSize(16);
			l.setMouseTransparent(true);
			top.getChildren().add(l);
			
			top.setOnMouseClicked((event)-> {
				if ( event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT )
					return;
				
				if ( inst == null )
					return;
				
				ContextMenu menu = new ContextMenu();
				MenuItem copyPath = new MenuItem("Copy Path");
				copyPath.setOnAction((clickEvent)->{
					LWJGUI.runLater(() -> {
						GLFW.glfwSetClipboardString(window.getID(), inst.getFullName());
					});
				});
				menu.getItems().add(copyPath);
				menu.show(top.getScene(), event.getMouseX(), top.getY()+top.getHeight());
			});
	
			// Create column constraints
			ColumnConstraint const1 = new ColumnConstraint( 100 );
			const1.setFillWidth(true);
			ColumnConstraint const2 = new ColumnConstraint( 0, Priority.ALWAYS );
			
			// Create grid
			internal = new GridPane();
			internal.setBackgroundLegacy(Theme.current().getBackgroundAlt());
			internal.setFillToParentWidth(true);
			internal.setHgap(1);
			internal.setVgap(1);
			internal.setColumnConstraint(0, const1);
			internal.setColumnConstraint(1, const2);

			this.getChildren().add(internal);
		}
		
		@Override
		public void resize() {
			//this.setMaxWidth(scroller.getViewportWidth());
			this.setPrefWidth(scroller.getViewportWidth());
			internal.forceWidth(this.getPrefWidth());
			super.resize();
			
			if ( this.inst != null && this.inst.isDestroyed() ) {
				this.clear();
			}
		}
		
		private void fill() {
			// Fill grid
			LuaValue[] fields = inst.getFieldsOrdered();
			for (int i = 0; i < fields.length; i++) {
				LuaValue field = fields[i];
				LuaValue value = inst.get(field);
				
				String fieldName = field.toString();
				
				Label fieldLabel = new Label(fieldName);
				fieldLabel.setFontSize(16);
				
				// Cell 1
				StackPane t1 = new StackPane();
				t1.setBackgroundLegacy(Theme.current().getPane());
				if ( i % 2 == 1 )
					t1.setBackgroundLegacy(Theme.current().getPaneAlt());
				t1.setPadding(new Insets(2,0,2,12));
				t1.setPrefSize(1, 1);
				t1.getChildren().add(fieldLabel);
				
				// Cell 2
				PropertyModifier t2 = getPropertyModifier( inst, fieldName, value );
				if ( t2 == null )
					return;
				
				t2.setBackground(t1.getBackground());
				
				// Make cell 1 text color match cell 2
				boolean editable = inst.getField(field).canModify();
				if ( t2 instanceof PropertyModifierTemp )
					fieldLabel.setTextFill(((PropertyModifierTemp) t2).label.getTextFill());
				if ( !editable )
					fieldLabel.setTextFill(Color.GRAY);
				
				// Add them to grid
				getInternal().add(t1, 0, i);
				getInternal().add(t2, 1, i);
				props.put(field, t2);
			}
			
			this.updateChildren();
			this.position(this.getParent());
			this.position(this.getParent());
			//this.render(this.cached_context);
			//this.render(this.cached_context);
			this.updateChildrenLocalRecursive();
			
			this.internal.updateChildren();
		}

		public void update(Instance instance, LuaValue property, LuaValue value) {
			//if ( SELECTING_OBJECT )
				//return;
			
			if ( inst != null && !instance.equals(inst))
				return;
			
			if ( instance.getFields().length != props.size() ) {
				this.inst = null;
				setInstance(instance);
				return;
			}
			
			// Hack to prevent script source
			if ( instance instanceof ScriptBase && property.toString().equals("Source") )
				return;
			
			PropertyModifier p = props.get(property);
			if ( p == null ) {
				return;
			}
			
			p.update(value);
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
			if ( inst == this.inst )
				return;
			
			this.inst = inst;
			l.setText(inst.getFullName());
			
			fill();
		}
	}
}
