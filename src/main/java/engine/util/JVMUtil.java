package engine.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import engine.InternalGameThread;

public class JVMUtil {
	public static boolean restartJVM(boolean startFirstThread, boolean needsOutput, Class<?> customClass, String... args) {
		if ( startFirstThread ) {
			String startOnFirstThread = System.getProperty("XstartOnFirstThread");
			if ( startOnFirstThread != null && startOnFirstThread.equals("true") )
				return false;

			// if not a mac return false
			String osName = System.getProperty("os.name");
			if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
				return false;
			}
		}

		// get current jvm process pid
		String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		// get environment variable on whether XstartOnFirstThread is enabled
		String env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid);

		// if environment variable is "1" then XstartOnFirstThread is enabled
		if (env != null && env.equals("1") && startFirstThread) {
			return false;
		}

		// restart jvm with -XstartOnFirstThread
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String mainClass = System.getenv("JAVA_MAIN_CLASS_" + pid);
		String jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java";

		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

		ArrayList<String> jvmArgs = new ArrayList<String>();


		jvmArgs.add(jvmPath);
		if ( startFirstThread )
			jvmArgs.add("-XstartOnFirstThread");
		jvmArgs.addAll(inputArguments);
		jvmArgs.add("-cp");
		jvmArgs.add(classpath);

		if ( customClass == null ) {
			jvmArgs.add(mainClass);
		} else {
			jvmArgs.add(customClass.getName());
		}
		for (int i = 0; i < args.length; i++) {
			jvmArgs.add(args[i]);
		}

		// if you don't need console output, just enable these two lines
		// and delete bits after it. This JVM will then terminate.
		if ( !needsOutput ) {
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
				processBuilder.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();

				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				while ((line = br.readLine()) != null) {
					Thread.sleep(10);
					Thread.yield();
					System.out.println(line);
				}
				System.exit(0);
				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/** restart jvm with -XstartOnFirstThread if on MACOS */
	public static void newJVM( Class<?> customClass, String... args ) {
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java";

		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
		ArrayList<String> jvmArgs = new ArrayList<String>();

		// Initial arguments
		jvmArgs.add(jvmPath);
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Mac") || osName.startsWith("Darwin"))
			jvmArgs.add("-XstartOnFirstThread");
		jvmArgs.addAll(inputArguments);
		jvmArgs.add("-cp");
		jvmArgs.add(classpath);
		jvmArgs.add(customClass.getName());
		
		// User arguments
		for (int i = 0; i < args.length; i++) {
			jvmArgs.add(args[i]);
		}
		
		// Launch!
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
					processBuilder.redirectErrorStream(true);
					Process process = processBuilder.start();

					InputStream is = process.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					String line;

					while ((line = br.readLine()) != null && InternalGameThread.isRunning()) {
						Thread.sleep(10);
						System.out.println(line);
					}
					//process.waitFor();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}).start();
	}
}
