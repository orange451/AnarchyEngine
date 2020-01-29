package engine.gl.exceptions;

public class IncludeShaderException extends RuntimeException {

	private static final long serialVersionUID = -884808269247123167L;

	public IncludeShaderException() {
		super();
	}

	public IncludeShaderException(String error) {
		super(error);
	}

	public IncludeShaderException(Exception e) {
		super(e);
	}

	public IncludeShaderException(Throwable cause) {
		super(cause);
	}

	public IncludeShaderException(String message, Throwable cause) {
		super(message, cause);
	}

}
