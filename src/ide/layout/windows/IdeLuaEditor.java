package ide.layout.windows;

import engine.lua.type.object.ScriptBase;
import engine.lua.type.object.insts.Script;
import ide.layout.IdePane;
import lwjgui.scene.Node;
import lwjgui.scene.control.CodeArea;

public class IdeLuaEditor extends IdePane {
	private ScriptBase inst;
	private CodeArea code;
	
	public IdeLuaEditor(ScriptBase script) {
		super(script.getName()+".lua", true);
		
		code = new CodeArea();
		code.setPreferredColumnCount(1024);
		code.setPreferredRowCount(1024);
		this.getChildren().add(code);
		
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
