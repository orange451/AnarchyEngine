package engine.util;

import java.math.BigDecimal;

public class Misc {
	public static BigDecimal truncateDecimal(double x,int numberofDecimals) {
	    if ( x > 0) {
	        return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_FLOOR);
	    } else {
	        return new BigDecimal(String.valueOf(x)).setScale(numberofDecimals, BigDecimal.ROUND_CEILING);
	    }
	}
}
