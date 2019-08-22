package engine.util;

import org.lwjgl.nanovg.NVGColor;

public class NanoVGUtil {
	public static NVGColor color( int r, int g, int b, int a ) {
		NVGColor ret = NVGColor.create();
		ret.r(r/255.0f);
		ret.g(g/255.0f);
		ret.b(b/255.0f);
		ret.a(a/255.0f);
		
		return ret;
	}
}
