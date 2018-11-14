package ide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.lwjgl.glfw.GLFW;

import engine.application.RenderableApplication;
import lwjgui.LWJGUI;
import lwjgui.LWJGUIUtil;
import lwjgui.geometry.Insets;
import lwjgui.scene.Window;
import lwjgui.scene.control.Button;
import lwjgui.scene.control.Label;
import lwjgui.scene.layout.BorderPane;

public class ErrorWindow {
	
	private static final int WIDTH = 300;
	private static final int HEIGHT = 120;

	public ErrorWindow(String string) {
		if ( RenderableApplication.GLFW_INITIALIZED ) {
			errorLWJGUI(string);
		} else {
			errorSwing(string);
		}
	}
	
	private void errorSwing(String string) {
		JFrame.setDefaultLookAndFeelDecorated(false);
		try {
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			//e.printStackTrace();
		}

		final JFrame frame = new JFrame("Error!");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable( false );

		// Center frame
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2 - WIDTH/2);
		int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2 - HEIGHT/2);
		frame.setLocation(x, y);


		//This button lets you close the window.
		JButton button = new JButton("Ok");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.setVisible(false);
				frame.dispose();
				//clicked = true;
				//System.exit(0);
			}

		});

		JPanel messagePane = new JPanel();
		messagePane.add(new JLabel(string));
		frame.getContentPane().add(messagePane);
		JPanel buttonPane = new JPanel();
		buttonPane.add(button);
		frame.getContentPane().add(buttonPane, BorderLayout.SOUTH);


		//Show window.
		frame.setSize(new Dimension(WIDTH, HEIGHT));
		frame.setResizable(false);
		//frame.pack();
		frame.setVisible(true);

		/*while ( !clicked ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//
			}
		}*/
	}

	private void errorLWJGUI(String string) {
		long win = LWJGUIUtil.createOpenGLCoreWindow("Error", WIDTH, HEIGHT, false, true);
		Window window = LWJGUI.initialize(win);
		
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(4,4,4,4));
		window.getScene().setRoot(root);
		
		Label l = new Label(string);
		root.setCenter(l);
		
		Button b = new Button("Ok");
		b.setMinWidth(64);
		root.setBottom(b);
		
		b.setOnAction(event -> {
			GLFW.glfwSetWindowShouldClose(win, true);
		});
	}
}
