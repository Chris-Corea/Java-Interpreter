import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Scanner;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class jInterp {

	public static final String newline = System.getProperty("line.separator");
	public static final String pathSep = System.getProperty("path.separator");

	public String tmpdir = null;
	public DiagnosticCollector<JavaFileObject> diagnostics;
	public boolean exec = false;

	ClassLoader loader;

	public static final String CLASSPATH = System
			.getProperty("java.class.path");

	public static void main(String[] args) {

		Scanner scan = new Scanner(System.in);
		String code;
		jInterp myCompiler = new jInterp();
		int classNumber = 0;
		boolean declaration = true;

		try {
			myCompiler.setUp();
			myCompiler.loader = new URLClassLoader(new URL[] { new File(
					myCompiler.tmpdir).toURI().toURL() },
					ClassLoader.getSystemClassLoader());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Could not set up interpreter environment.");
		}

		System.out.print(">>> ");
		while (scan.hasNextLine()) {
			code = scan.nextLine();
			boolean success = false;
			if (!code.equals("")) {
				try {
					success = myCompiler.compile(myCompiler.javaFileTemplate(
							classNumber, code, declaration), classNumber);
					if (!success) {
						declaration = false;
						success = myCompiler.compile(myCompiler
								.javaFileTemplate(classNumber, code,
										declaration), classNumber);
					}
					if (!success) {
						// syntactical error
						myCompiler.dumpDiagonostics();
						System.out.println("\nSyntax error!");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (myCompiler.exec) {
					myCompiler.run("Interp_" + classNumber);
					myCompiler.exec = false;
					declaration = true;
				}

				if (success)
					classNumber++;
				System.out.print(">>> ");
			}
		}

		System.out.println();
	}

	public void run(String className) {
		try {
			final Class<?> mainClass = (Class<?>) loader.loadClass(className);
			Object main = mainClass.newInstance();
			Method mainMethod = mainClass.getMethod("exec", null);
			mainMethod.invoke(null, (Object[]) null);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found: " + e);
		} catch (NoSuchMethodException e) {
			System.err.println("No such method: " + e);
		} catch (IllegalAccessException e) {
			System.err.println("Illegal access: " + e);
		} catch (InvocationTargetException e) {
			System.err.println("Invocation target: " + e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns "java file" for encoding using the template within the method.
	 * 
	 * @param num
	 *            Number to set current "Interp" class e.g. num = 0 :: Interp_0;
	 *            num = 3 :: Interp_3
	 * @param code
	 *            Compilable code
	 * @param declaration
	 *            flag to determine whether or not we should assume code is a
	 *            declaration
	 * @return
	 */
	public String javaFileTemplate(int num, String code, boolean declaration) {
		String template = "";
		if (num == 0) {
			template = "import java.io.*;\n" + "import java.util.*;\n"
					+ "public class Interp_" + num + " {\n";
			if (declaration) {
				if (!code.startsWith("public static")) {
					template = template
							+ "" + "	public static " + code + "\n"
									+ "	public static void exec() {\n" + "	}\n" + "}";
				} else {
					template = template
							+ "" + "	" + code + "\n"
									+ "	public static void exec() {\n" + "	}\n" + "}";
				}
			} else { // must be a function call
				template = template
						+ "	public static void exec() {\n\t\t" + code
								+ "\n\t} \n" + "}";
				exec = true;
			}
		} else {
			template = "import java.io.*;\n" + "import java.util.*;\n"
					+ "public class Interp_" + num + " extends Interp_"
					+ (num - 1) + " {\n";
			if (declaration) {
				if (!code.startsWith("public static")) {
					template = template
							+ "" + "	public static " + code + "\n"
									+ "	public static void exec() {\n" + "	}\n" + "}";
				} else {
					template = template
							+ "" + "	" + code + "\n"
									+ "	public static void exec() {\n" + "	}\n" + "}";
				}
			} else { // must be a function call
				template = template
						+ "	public static void exec() {\n\t\t" + code
								+ "\n\t} \n" + "}";
				exec = true;
			}
		}
		return template;
	}

	public void setUp() throws Exception {
		tmpdir = System.getProperty("java.io.tmpdir");
	}

	/**
	 * Compiles Java code in <i>code</i> to a proper Java class file. The class
	 * file is named Interp_<i>classNumber</i> where <i>classNumber</i> is the
	 * number of files compiled throughout execution of the program (i.e.
	 * Interp_0, Interp_1).
	 * 
	 * @param code
	 *            - Java source code to compiled
	 * @param classNumber
	 *            - specifies the number for the name of the compiled Java class
	 * @return true if and only if the file compiled successfully; false
	 *         otherwise
	 */
	protected boolean compile(String code, int classNumber) {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		diagnostics = new DiagnosticCollector<JavaFileObject>();

		JavaFileObject files = new JavaSourceFromString(
				("Interp_" + classNumber), code);

		Iterable<? extends JavaFileObject> compilationUnits = Arrays
				.asList(files);

		Iterable<String> compileOptions = Arrays.asList("-d", tmpdir, "-cp",
				tmpdir + pathSep + CLASSPATH);

		JavaCompiler.CompilationTask task = compiler.getTask(null, null,
				diagnostics, compileOptions, null, compilationUnits);
		boolean ok = task.call();

		return ok;
	}

	/**
	 * Dumps compilation errors to standard output after failed attempt to
	 * compile a file.
	 */
	public void dumpDiagonostics() {
		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			System.out.format("Error on line %d in %s",
					diagnostic.getLineNumber(), diagnostic);
		}
	}

	/**
	 * A file object used to represent source coming from a string.
	 * 
	 * ***** Code adapted from
	 * http://docs.oracle.com/javase/6/docs/api/javax/tools/JavaCompiler.html
	 * *****
	 */
	public class JavaSourceFromString extends SimpleJavaFileObject {

		/**
		 * The source code of this "file".
		 */
		final String code;
		/**
		 * The name of this "file".
		 */
		final String qualifiedName;

		/**
		 * Constructs a new JavaSourceFromString.
		 * 
		 * @param name
		 *            the name of the compilation unit represented by this file
		 *            object
		 * @param code
		 *            the source code for the compilation unit represented by
		 *            this file object
		 */
		public JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/')
					+ Kind.SOURCE.extension), Kind.SOURCE);
			this.qualifiedName = name;
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}

	}

}
