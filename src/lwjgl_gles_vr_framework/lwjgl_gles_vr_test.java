package lwjgl_gles_vr_framework;

import static org.lwjgl.opengles.GLES30.glBindVertexArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import org.lwjgl.opengles.*;
import org.lwjgl.opengles.GLES.*;
//import org.lwjgl.opengles.GLES32.*;
import org.lwjgl.opengles.OESMapbuffer.*;
import org.lwjgl.system.*;
import org.lwjgl.egl.*;
import org.lwjgl.glfw.*;
//import org.lwjgl.opengl.GLUtil.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeEGL.*;
import static org.lwjgl.egl.EGL10.*;
import static org.lwjgl.opengles.GLES20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class lwjgl_gles_vr_test implements Runnable
{

	public static void main(String[] args) {
		System.out.println("Welcome!");

		System.loadLibrary("libGLESv2");
		System.loadLibrary("libEGL");

		lwjgl_gles_vr_test l_lgvt = new lwjgl_gles_vr_test();
		l_lgvt.run();
	}

	private int displayW;
	private int displayH;
	private long m_window;
	private EGLCapabilities m_egl;
	private GLESCapabilities m_gles;
	private int m_main_program;
	private int m_point_program;
	private int m_point_program_col;
	private boolean m_finished = false;
	private IntBuffer m_intbuffer;
	private double m_rot = 0.0f;
	private float m_Scale;
	private float m_Pointsize;
	private long lastTime;
	private boolean m_stoprotation;

	private void render(EGLCapabilities egl, GLESCapabilities gles) {

		int modelViewLoc;
		int scaleLoc;

		int errorCheckValue;

		// Create a simple quad
		int vbo = glGenBuffers();
		int ibo = glGenBuffers();
		float[] vertices = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, // Red X
				250.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
				
				0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,// Green Y
				0.0f, 500.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,

				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Blue Z
				0.0f, 0.0f, 250.0f, 0.0f, 0.0f, 1.0f, 0.0f,
				0 };
		int[] indices = { 1, 0, 2, 3, 4, 5 };
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(vertices.length).put(vertices).flip(),
				GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4 + 4 * 4, 0);
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 3 * 4 + 4 * 4, 3 * 4);
		glEnableVertexAttribArray(1);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER,
				(IntBuffer) BufferUtils.createIntBuffer(indices.length).put(indices).flip(), GL_STATIC_DRAW);

		glClearColor(0.0f, 0.5f, 1.0f, 0.0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glViewport(0, 0, displayW, displayH);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("GL Error " + errorCheckValue);
			Thread.dumpStack();
			return;
		}

		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CCW);

		glDepthRangef(0.0f, 1.0f);
		if (!GLok("Setting glDepthRange"))
			return;
		glEnable(GL_DEPTH_TEST);

		glDisable(GL_CULL_FACE);
		if (!GLok("Disabling GL_CULL_FACE"))
			return;

		Matrix4f projectM = new Matrix4f();
		Matrix4f viewM = new Matrix4f();
		Matrix4f modelM = new Matrix4f();
		Matrix4f modelView = new Matrix4f();
		FloatBuffer fb = BufferUtils.createFloatBuffer(16);

		float l_x_scale = 1.0f;
		float l_y_scale = 1.0f;
		if(displayW>displayH)
		{
			l_x_scale = (float)displayH/(float)displayW;
		}
		else
		{
			l_y_scale = (float)displayW/(float)displayH;			
		}
		
		
/*		
		float aspect = width / height;
		glViewport(0, 0, width, height);
		if (aspect >= 1.0)
		  glOrtho(-50.0 * aspect, 50.0 * aspect, -50.0, 50.0, 1.0, -1.0);
		else
		  glOrtho(-50.0, 50.0, -50.0 / aspect, 50.0 / aspect, 1.0, -1.0);*/
		
		
		
		
		projectM
				.setOrtho(-1000.0f/l_x_scale, 1000.0f/l_x_scale, -1000.0f/l_y_scale, 1000.0f/l_y_scale, -30000.0f, 30000.0f);
		viewM.identity();
		// User controlled front view
		viewM.lookAt(0.0f, 5000.0f, 15000.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		
		// Corner view
		//viewM.lookAt(0.0f, 100.0f, 100.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
		// Top view
		//viewM.lookAt(0.0f, 1000.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
		modelM.identity();
		Quaternionf q = new Quaternionf();
		modelM.rotate(q.rotateY((float) Math.toRadians(m_rot )).normalize());

		glUseProgram(m_main_program);
		if (!GLok(""))
			return;
		modelViewLoc = glGetUniformLocation(m_main_program, "modelView");
		if (!GLok(""))
			return;
		modelView.identity();
		modelView.mul(projectM);
		modelView.mul(viewM);
		modelView.mul(modelM);
//		modelView = projectM.mul(viewM).mul(modelM);
		glUniformMatrix4fv(modelViewLoc, false, modelView.get(fb));
		if (!GLok(""))
			return;
		glBindAttribLocation(m_main_program, 0, "vertex");
		if (!GLok(""))
			return;
		glBindAttribLocation(m_main_program, 0, "color");
		if (!GLok(""))
			return;
		glDrawElements(GL_LINES, 6, GL_UNSIGNED_INT, 0);
		if (!GLok(""))
			return;

//		modelM.rotate(q.rotateY((float) Math.toRadians(m_rot)).normalize());
		//modelM.rotate(q.rotateX((float) Math.toRadians(m_rot)).normalize());
//				.translate(m_pcd.x_pos, m_pcd.y_pos, m_pcd.z_pos)

		/* .rotate(q.rotateZ((float) Math.toRadians(rot)).normalize()) */;
		int l_program;
/*		if(m_pcd.m_layers.get(i).fixedColor)
		{
			glUseProgram(m_point_program);
			l_program = m_point_program;
			if (!GLok("glUseProgram(m_point_program)"))
				return;
			int l_colorloc = glGetUniformLocation(m_point_program, "color");
			GLok("Retrieving scale uniform location");
				glUniform4f(l_colorloc,
						m_pcd.m_layers.get(i).r/255.0f,
						m_pcd.m_layers.get(i).g/255.0f,
						m_pcd.m_layers.get(i).b/255.0f,
						0.0f
						);
		}
		else*/
	//	{
			glUseProgram(m_point_program_col);
			l_program = m_point_program_col;
			if (!GLok("glUseProgram(m_point_program_col)"))
				return;
			glBindAttribLocation(m_point_program, 1, "color");
			if (!GLok("Setting glBindAttribLocation"))
				return;
		//}
		scaleLoc = glGetUniformLocation(l_program, "scale");
		GLok("Retrieving scale uniform location");
		//System.out.println("Scale to "+((float)sbScale.getValue()/10.0f));
		glUniform1f(scaleLoc, (m_Scale/1000.0f)); 
		GLok("Set scale uniform");
		int pointsizeLoc = glGetUniformLocation(l_program, "pointsize");
		GLok("Retrieving pointsize uniform location");
		//System.out.println("Point size to "+sbPointSize.getValue()/10);
		glUniform1f(pointsizeLoc, m_Pointsize/10.0f); 
		GLok("Set pointsize uniform");

		glBindAttribLocation(l_program, 0, "vertex");
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("GL Error " + errorCheckValue);
			Thread.dumpStack();
			return;
		}
		/*
		for(int i=0; i<m_points.size(); ++i)
		{
//			private int m_tt_angle;
//			private int m_Zrotoff;
//			private int m_Xrotoff;

			
			
			modelM.identity();
			q = new Quaternionf();
			modelM.rotate(q.rotateY((float) Math.toRadians(i*m_tt_angle+m_rot)).normalize());
			modelM.translate(m_Xrotoff, m_Ydispoff, -m_Zrotoff);
			//modelM.translate(0.0f, 0.0f, -2000.0f);
			modelViewLoc = glGetUniformLocation(l_program, "modelView");
			if (!GLok("Calling glGetUniformLocation"))
				return;
			modelView.identity();
			modelView.mul(projectM);
			modelView.mul(viewM);
			modelView.mul(modelM);
			glUniformMatrix4fv(modelViewLoc, false, modelView.get(fb));
			if (!GLok("Setting glUniformMatrix4fv"))
				return;
			draw(i);
		}
*/
		long thisTime = System.nanoTime();
		float delta = (thisTime - lastTime) / 1E9f;
		if(!m_stoprotation) m_rot += delta * 5f;
		if (m_rot > 360.0f) {
			m_rot = 0.0f;
		}
		//m_rot = 176.0f;
//		System.out.println("Rot: "+m_rot);
		lastTime = thisTime;

	}
	
	boolean GLok(String message) {
		int errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("GL Error " + errorCheckValue + " " + message);
			Thread.dumpStack();
			return false;
		}
		return true;
	}

	private static String readFileAsString(String filename) throws Exception {
		StringBuilder source = new StringBuilder();

		FileInputStream in = new FileInputStream(filename);

		Exception exception = null;

		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

			Exception innerExc = null;
			try {
				String line;
				while ((line = reader.readLine()) != null)
					source.append(line).append('\n');
			} catch (Exception exc) {
				exception = exc;
			} finally {
				try {
					reader.close();
				} catch (Exception exc) {
					if (innerExc == null)
						innerExc = exc;
					else
						exc.printStackTrace();
				}
			}

			if (innerExc != null)
				throw innerExc;
		} catch (Exception exc) {
			exception = exc;
		} finally {
			try {
				in.close();
			} catch (Exception exc) {
				if (exception == null)
					exception = exc;
				else
					exc.printStackTrace();
			}

			if (exception != null)
				throw exception;
		}

		return source.toString();
	}

	private int shaderInit(String vsn, String fsn) {

		String vertexShader;
		String fragmentShader;
		int errorCheckValue;
		String l_dir = "src/lwjgl_gles_vr_framework/";
		try {
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			System.out.println("Current relative path is: " + s);
			vertexShader = readFileAsString(l_dir + vsn);
			fragmentShader = readFileAsString(l_dir + fsn);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}

		int program = glCreateProgram();
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to create shader program");
			return -1;
		}
		int vs = glCreateShader(GL_VERTEX_SHADER);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to create vertex shader");
			return -1;
		}
		glShaderSource(vs, vertexShader);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to load vertex shader string");
			return -1;
		}
		glCompileShader(vs);
		int isCompiled;
		isCompiled = glGetShaderi(vs, GL_COMPILE_STATUS);
		if (isCompiled == GL_FALSE) {
			String shaderLog = glGetShaderInfoLog(vs);
			System.err.println("Failed to compile vertex shader");
			System.err.println(shaderLog);
			return -1;
		}
		glAttachShader(program, vs);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to attach vertex shader to program");
			return -1;
		}

		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to create fragment shader");
			return -1;
		}
		glShaderSource(fs, fragmentShader);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to load fragment shader");
			return -1;
		}
		glCompileShader(fs);
		isCompiled = glGetShaderi(fs, GL_COMPILE_STATUS);
		if (isCompiled == GL_FALSE) {
			String shaderLog = glGetShaderInfoLog(fs);
			System.err.println("Failed to compile fragment shader");
			System.err.println(shaderLog);
			return -1;
		}
		glAttachShader(program, fs);
		errorCheckValue = glGetError();
		if (errorCheckValue != GL_NO_ERROR) {
			System.err.println("Failed to attach fragment shader to program");
			return -1;
		}
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			System.err.println("Failed to link program");
			System.err.println(glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH)));
			return -1;
		}

		System.out.println("Created program Id " + program);
		return program;
	}

	@Override
	public void run()
	{
		// Render with OpenGL ES
	
		System.out.println("Run");
		float scale = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 96.0f;
		System.out.println("Res: " + java.awt.Toolkit.getDefaultToolkit().getScreenResolution());
		displayW = 1000;
		displayH = 680;
		/*
		 * try (MemoryStack stack = stackPush()) { IntBuffer peError =
		 * stack.mallocInt(1);
		 * 
		 * int token = VR_InitInternal(peError, 0); if (peError.get(0) == 0) { try {
		 * OpenVR.create(token);
		 * 
		 * System.err.println("Model Number : " +
		 * VRSystem_GetStringTrackedDeviceProperty( k_unTrackedDeviceIndex_Hmd,
		 * ETrackedDeviceProperty_Prop_ModelNumber_String, peError));
		 * System.err.println("Serial Number: " +
		 * VRSystem_GetStringTrackedDeviceProperty( k_unTrackedDeviceIndex_Hmd,
		 * ETrackedDeviceProperty_Prop_SerialNumber_String, peError));
		 * 
		 * IntBuffer w = stack.mallocInt(1); IntBuffer h = stack.mallocInt(1);
		 * VRSystem_GetRecommendedRenderTargetSize(w, h);
		 * System.err.println("Recommended width : " + w.get(0));
		 * System.err.println("Recommended height: " + h.get(0)); displayW = w.get(0);
		 * displayH = h.get(0); } finally { VR_ShutdownInternal(); } } else {
		 * System.out.println("INIT ERROR SYMBOL: " +
		 * VR_GetVRInitErrorAsSymbol(peError.get(0)));
		 * System.out.println("INIT ERROR  DESCR: " +
		 * VR_GetVRInitErrorAsEnglishDescription(peError.get(0))); } }
		 */
		{
			// OpenGL ES 3.0 EGL init
			GLFWErrorCallback.createPrint().set();
			if (!glfwInit()) {
				throw new IllegalStateException("Unable to initialize glfw");
			}
	
			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
			glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
			glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);
			//glfwWindowHint(GLFW_FLOATING, GLFW_TRUE);
	
			// GLFW setup for EGL & OpenGL ES
			glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API);
			glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
	
			m_window = glfwCreateWindow(displayW, displayH, "glpanel", NULL, NULL);
			if (m_window == NULL) {
				throw new RuntimeException("Failed to create the GLFW window");
			}
			/*glfwSetWindowPos(m_window, (int) (m_glpanel.getLocationOnScreen().getX() * scale),
					(int) (m_glpanel.getLocationOnScreen().getY() * scale));*/
	
			glfwSetKeyCallback(m_window, (windowHnd, key, scancode, action, mods) -> {
				/*
				 * if (action == GLFW_RELEASE && key == GLFW_KEY_ESCAPE) {
				 * glfwSetWindowShouldClose(windowHnd, true); }
				 */
			});
			glfwSetWindowPosCallback(m_window, (windowHnd, xpos, ypos) -> {
				System.out.println("Moved by system to " + xpos + "," + ypos);
			});
			glfwSetWindowSizeCallback(m_window, (windowHnd, width, height) -> {
				System.out.println("Resized to " + width+ "," + height);
				displayW = width;
				displayH = height;
			});
	
			// EGL capabilities
			long dpy = glfwGetEGLDisplay();
	
			try (MemoryStack stack = stackPush()) {
				IntBuffer major = stack.mallocInt(1);
				IntBuffer minor = stack.mallocInt(1);
	
				if (!eglInitialize(dpy, major, minor)) {
					throw new IllegalStateException(String.format("Failed to initialize EGL [0x%X]", eglGetError()));
				}
	
				m_egl = EGL.createDisplayCapabilities(dpy, major.get(0), minor.get(0));
			}
	/*
			try {
				System.out.println("EGL Capabilities:");
				for (Field f : EGLCapabilities.class.getFields()) {
					if (f.getType() == boolean.class) {
						if (f.get(m_egl).equals(Boolean.TRUE)) {
							System.out.println("\t" + f.getName());
						}
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	*/
			// OpenGL ES capabilities
			glfwMakeContextCurrent(m_window);
			m_gles = GLES.createCapabilities();
	/*
			try {
				System.out.println("OpenGL ES Capabilities:");
				for (Field f : GLESCapabilities.class.getFields()) {
					if (f.getType() == boolean.class) {
						if (f.get(m_gles).equals(Boolean.TRUE)) {
							System.out.println("\t" + f.getName());
						}
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
	*/
			System.out.println("GL_VENDOR: " + glGetString(GL_VENDOR));
			System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
			System.out.println("GL_RENDERER: " + glGetString(GL_RENDERER));
	
			m_main_program = shaderInit("mainVertexShader.glsl", "mainFragmentShader.glsl");
			if (m_main_program == -1) {
				System.err.println("Failed to initialise main shaders");
				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
			m_point_program = shaderInit("pointVertexShader.glsl", "pointFragmentShader.glsl");
			if (m_point_program == -1) {
				System.err.println("Failed to initialise point shaders");
				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
			m_point_program_col = shaderInit("pointVertexShaderCol.glsl", "pointFragmentShaderCol.glsl");
			if (m_point_program_col == -1) {
				System.err.println("Failed to initialise colored point shaders");
				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
		}
		
		glfwShowWindow(m_window);
		
		while (!glfwWindowShouldClose(m_window)) {
			glfwPollEvents();
	/*			if (m_reload) {
				glfwMakeContextCurrent(m_window);
				m_pcos = new ArrayList<>();
				for (int i = 0; i < m_pcd.m_layers.size(); ++i) {
					m_pcos.add(new PointCloudObject(m_pcd, i));
					if (!m_pcos.get(i).load()) {
						System.err.println("Failed to load point cloud");
						GLES.setCapabilities(null);
	
						glfwFreeCallbacks(m_window);
						glfwTerminate();
						System.exit(1);
	
					}
				}
				m_reload = false;
				m_pause = false;
			}
			if (m_pause) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {*/
				//System.out.println("Render");
				render(m_egl, m_gles);
				//System.out.println("Rendered");
	
				glfwSwapBuffers(m_window);
				
	//		}
		}
		glclear();
	    glfwHideWindow(m_window);
		GLES.setCapabilities(null);
		glfwFreeCallbacks(m_window);
		glfwTerminate();
		m_finished = true;
	}

	
	public void glclear()
	{
    	glBindBuffer(GL_ARRAY_BUFFER, 0);
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    	if(m_intbuffer!=null) glDeleteBuffers(m_intbuffer);
	}

}
