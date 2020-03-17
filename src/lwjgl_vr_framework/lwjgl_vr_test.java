package lwjgl_vr_framework;

import static org.lwjgl.opengl.GL43.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import org.lwjgl.system.*;
//import org.lwjgl.egl.*;
import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeEGL.*;
//import static org.lwjgl.egl.EGL10.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.system.APIUtil.*;

import org.lwjgl.openvr.*;
import static org.lwjgl.openvr.VR.*;
import static org.lwjgl.openvr.VRSystem.*;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRCompositor;
import org.lwjgl.openvr.VRSystem;

class Eye
{
	int m_eye;

	int m_fbo;
	int m_fbo_texture;

	Matrix4f m_headposition_matrix;
	Matrix4f m_projection_matrix;
	Matrix4f m_eyetohead_matrix;
	
	Eye(int a_eye)
	{
		m_eye = a_eye;
		m_headposition_matrix = new Matrix4f();
		m_projection_matrix = new Matrix4f();
		m_eyetohead_matrix = new Matrix4f();
	}
}

public class lwjgl_vr_test implements Runnable
{

	public static void main(String[] args) {
		System.out.println("Welcome!");
		
		Configuration.DISABLE_CHECKS.set(false);
		Configuration.DEBUG.set(true);
		Configuration.DEBUG_LOADER.set(true);
		Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
		Configuration.DEBUG_STACK.set(true);
		Configuration.DEBUG_STREAM.set(System.err);

//		System.loadLibrary("libGLESv2");
//		System.loadLibrary("libEGL");
		
//		System.loadLibrary("vrclient_x64");
		
		lwjgl_vr_test l_lgvt = new lwjgl_vr_test();
		l_lgvt.run();
	}

	private int displayW;
	private int displayH;
	private long m_window;
//	private EGLCapabilities m_egl;
//	private GLESCapabilities m_gles;
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

	Eye m_left_eye, m_right_eye;
	
	private void render(Eye a_eye) {

		int modelViewLoc;
		int scaleLoc;

		int errorCheckValue;

		int VAO;
		VAO = glGenVertexArrays();
		glBindVertexArray(VAO);
		
		// Create a simple quad
		int vbo = glGenBuffers();
		int ibo = glGenBuffers();
		float[] vertices = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, // Red X
				1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
				
				0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,// Green Y
				0.0f, 2.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,

				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Blue Z
				0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
				0 };
		int[] indices = { 1, 0, 2, 3, 4, 5 };
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		if(!GLok("glBindBuffer")) return;
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(vertices.length).put(vertices).flip(),
				GL_STATIC_DRAW);
		if(!GLok("glBufferData")) return;
		
		glEnableVertexAttribArray(0);
		if(!GLok("glEnableVertexAttribArray")) return;
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4 + 4 * 4, 0);
		if(!GLok("glVertexAttribPointer")) return;
		
		glEnableVertexAttribArray(1);
		if(!GLok("glEnableVertexAttribArray")) return;
		glVertexAttribPointer(1, 4, GL_FLOAT, false, 3 * 4 + 4 * 4, 3 * 4);
		if(!GLok("glVertexAttribPointer")) return;
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		if(!GLok("glBindBuffer")) return;
		glBufferData(GL_ELEMENT_ARRAY_BUFFER,
				(IntBuffer) BufferUtils.createIntBuffer(indices.length).put(indices).flip(), GL_STATIC_DRAW);
		if(!GLok("glBufferData")) return;

		glClearColor(0.0f, 0.5f, 1.0f, 1.0f);
		if(!GLok("glClearColor"))
		{
			return;
		}
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		if(!GLok("glClear"))
		{
			return;
		}

		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glFrontFace(GL_CCW);

		//glDepthRangef(0.0f, 1.0f);
		//if (!GLok("Setting glDepthRange"))
		//	return;
		glEnable(GL_DEPTH_TEST);

		glDisable(GL_CULL_FACE);
		if (!GLok("Disabling GL_CULL_FACE"))
			return;

		Matrix4f modelM = new Matrix4f();
		Matrix4f modelView = new Matrix4f();
		FloatBuffer fb = BufferUtils.createFloatBuffer(16);

		modelM.identity();
		Quaternionf q = new Quaternionf();
		modelM.translate(0.0f, 0.0f, -5.0f);
		modelM.rotate(q.rotateY((float) Math.toRadians(m_rot )).normalize());

		glUseProgram(m_main_program);
		if (!GLok(""))
			return;
		modelViewLoc = glGetUniformLocation(m_main_program, "modelView");
		if (!GLok(""))
			return;
		modelView.identity();
		modelView.mul(a_eye.m_projection_matrix);
		modelView.mul(a_eye.m_eyetohead_matrix);
		modelView.mul(a_eye.m_headposition_matrix);
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

		glBindVertexArray(0);
		glDeleteBuffers(vbo);
		glDeleteBuffers(ibo);
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
		String l_dir = "src/lwjgl_vr_framework/";
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

    private static void printDetail(PrintStream stream, String type, String message) {
        stream.printf("\t%s: %s\n", type, message);
    }

    
    private static String getDebugSource(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API:
                return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                return "WINDOW SYSTEM";
            case GL_DEBUG_SOURCE_SHADER_COMPILER:
                return "SHADER COMPILER";
            case GL_DEBUG_SOURCE_THIRD_PARTY:
                return "THIRD PARTY";
            case GL_DEBUG_SOURCE_APPLICATION:
                return "APPLICATION";
            case GL_DEBUG_SOURCE_OTHER:
                return "OTHER";
            default:
                return APIUtil.apiUnknownToken(source);
        }
    }

    private static String getDebugType(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR:
                return "ERROR";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                return "DEPRECATED BEHAVIOR";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                return "UNDEFINED BEHAVIOR";
            case GL_DEBUG_TYPE_PORTABILITY:
                return "PORTABILITY";
            case GL_DEBUG_TYPE_PERFORMANCE:
                return "PERFORMANCE";
            case GL_DEBUG_TYPE_OTHER:
                return "OTHER";
            case GL_DEBUG_TYPE_MARKER:
                return "MARKER";
            default:
                return APIUtil.apiUnknownToken(type);
        }
    }

    private static String getDebugSeverity(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH:
                return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM:
                return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW:
                return "LOW";
            case GL_DEBUG_SEVERITY_NOTIFICATION:
                return "NOTIFICATION";
            default:
                return APIUtil.apiUnknownToken(severity);
        }
    }

	void makeFBOTexture(Eye a_eye, int a_width, int a_height)
	{
		int l_depthbuffer;
		
		a_eye.m_fbo = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, a_eye.m_fbo);
		if (!GLok("Bind Framebuffer"))
		{
			a_eye.m_fbo = 0;
			return;
		}		
		l_depthbuffer = glGenRenderbuffers();
		if (!GLok("glGenRenderbuffers"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glBindRenderbuffer(GL_RENDERBUFFER, l_depthbuffer);
		if (!GLok("l_depthbuffer"))
		{
			a_eye.m_fbo = 0;
			return;
		}
	
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, a_width, a_height);
		if (!GLok("glRenderbufferStorage"))
		{
			a_eye.m_fbo = 0;
			return;
		}

		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, l_depthbuffer);
		if (!GLok("glFramebufferRenderbuffer"))
		{
			a_eye.m_fbo = 0;
			return;
		}

		a_eye.m_fbo_texture = glGenTextures();
		if (!GLok("glGenTextures"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glBindTexture(GL_TEXTURE_2D, a_eye.m_fbo_texture);
		if (!GLok("glBindTexture"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		if (!GLok("GL_TEXTURE_MIN_FILTER"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		if (!GLok("GL_TEXTURE_MAG_FILTER"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
		if (!GLok("GL_TEXTURE_BASE_LEVEL"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
		if (!GLok("GL_TEXTURE_MAX_LEVEL"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, a_width, a_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer)null);
		if (!GLok("glTexImage2D"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, a_eye.m_fbo_texture, 0);
		if (!GLok("glFramebufferTexture2D"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		
		int l_err = glCheckFramebufferStatus(GL_FRAMEBUFFER);

		glBindFramebuffer( GL_FRAMEBUFFER, 0 );
		if (!GLok("glBindFramebuffer"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		glBindTexture(GL_TEXTURE_2D, 0);
		if (!GLok("glBindTexture"))
		{
			a_eye.m_fbo = 0;
			return;
		}
		
		if(l_err != GL_FRAMEBUFFER_COMPLETE)
		{
			a_eye.m_fbo = 0;
			System.out.println("Failed to create FBO: "+l_err);
			return;
		}
		else
		{
			System.out.println("Framebuffer creation successful");
			return;
		}
	}
	
	BufferedImage getBufferedImageFromFBO(int a_width, int a_height)
	{
		ByteBuffer l_buffer = BufferUtils.createByteBuffer(a_width * a_height * 4);
		glReadPixels(0, 0, a_width, a_height, GL_RGBA, GL_UNSIGNED_BYTE, l_buffer);
		GLok("glReadPixels");
		BufferedImage l_image = new BufferedImage(a_width, a_height, BufferedImage.TYPE_3BYTE_BGR);
		byte[] array = ((DataBufferByte) l_image.getRaster().getDataBuffer()).getData();
		for(int y = 0; y < a_height; ++y)
		{
			for(int x = 0; x < a_width; ++x)
			{
				int i = (y*a_width + x)*3;
				int j = ((a_height-y-1)*a_width + x)*4;
				array[i+0] = l_buffer.get(j+2);
				array[i+1] = l_buffer.get(j+1);
				array[i+2] = l_buffer.get(j+0);
			}
		}
		return l_image;
	}
	
	void HmdMatrix34toMatrix4f(HmdMatrix34 a_in, Matrix4f a_out)
	{
		a_out.set(a_in.m(0), a_in.m(4), a_in.m(8),  0f, 
	              a_in.m(1), a_in.m(5), a_in.m(9),  0f, 
	              a_in.m(2), a_in.m(6), a_in.m(10), 0f, 
	              a_in.m(3), a_in.m(7), a_in.m(11), 1f);
	}
	
	void HmdMatrix44toMatrix4f(HmdMatrix44 a_in, Matrix4f a_out)
	{
		a_out.set(a_in.m(0), a_in.m(4), a_in.m(8),  a_in.m(12), 
	              a_in.m(1), a_in.m(5), a_in.m(9),  a_in.m(13), 
	              a_in.m(2), a_in.m(6), a_in.m(10), a_in.m(14), 
	              a_in.m(3), a_in.m(7), a_in.m(11), a_in.m(15)); 
	}
	/*
	void HmdMatrix34toMatrix4f(HmdMatrix34 a_in, Matrix4f a_out)
	{
		a_out.set(a_in.m(0), a_in.m(1), a_in.m(2), a_in.m(3), 
	            a_in.m(4), a_in.m(5), a_in.m(6), a_in.m(7), 
	            a_in.m(8), a_in.m(9), a_in.m(10), a_in.m(11), 
	            0f, 0f, 0f, 1f);
	}
	
	void HmdMatrix44toMatrix4f(HmdMatrix44 a_in, Matrix4f a_out)
	{
		a_out.set(a_in.m(0), a_in.m(1), a_in.m(2), a_in.m(3), 
	            a_in.m(4), a_in.m(5), a_in.m(6), a_in.m(7), 
	            a_in.m(8), a_in.m(9), a_in.m(10), a_in.m(11), 
	            a_in.m(12), a_in.m(13), a_in.m(14), a_in.m(14)); 
	}
	*/
	@Override
	public void run()
	{
		// Render with OpenGL ES
	
		System.out.println("Run");
		float scale = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 96.0f;
		System.out.println("Res: " + java.awt.Toolkit.getDefaultToolkit().getScreenResolution());
		
		System.err.println("VR_IsRuntimeInstalled() = " + VR_IsRuntimeInstalled());
		System.err.println("VR_RuntimePath() = " + VR_RuntimePath());
		System.err.println("VR_IsHmdPresent() = " + VR_IsHmdPresent());

		int l_hmd_width = 16;
		int l_hmd_height = 16;
		
		m_left_eye = new Eye(VR.EVREye_Eye_Left);
		m_right_eye = new Eye(VR.EVREye_Eye_Right);
		m_left_eye.m_eye = VR.EVREye_Eye_Left;
		m_right_eye.m_eye = VR.EVREye_Eye_Right;
		
		try (MemoryStack stack = stackPush())
		{
			IntBuffer peError =
					stack.mallocInt(1);
		  
			int token = VR_InitInternal(peError, VR.EVRApplicationType_VRApplication_Scene);
			if (peError.get(0) == 0)
			{
				OpenVR.create(token);
	  			System.err.println("Model Number : " +
	  					VRSystem_GetStringTrackedDeviceProperty( k_unTrackedDeviceIndex_Hmd, ETrackedDeviceProperty_Prop_ModelNumber_String, peError));
	  			System.err.println("Serial Number: " +
	  					VRSystem_GetStringTrackedDeviceProperty( k_unTrackedDeviceIndex_Hmd, ETrackedDeviceProperty_Prop_SerialNumber_String, peError));

	  			IntBuffer w = stack.mallocInt(1);
	  			IntBuffer h = stack.mallocInt(1);
	  			VRSystem_GetRecommendedRenderTargetSize(w, h);
	  			System.err.println("Recommended width : " + w.get(0));
	  			System.err.println("Recommended height: " + h.get(0));
	  			l_hmd_width = w.get(0);
	  			l_hmd_height = h.get(0);
	  			displayW = l_hmd_width/2;
	  			displayH = l_hmd_height/2;
			} else
			{
				System.out.println("INIT ERROR SYMBOL: " +
						VR_GetVRInitErrorAsSymbol(peError.get(0)));
				System.out.println("INIT ERROR  DESCR: " +
						VR_GetVRInitErrorAsEnglishDescription(peError.get(0)));
				System.exit(2);
			}
		}
		
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
/*			glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_EGL_CONTEXT_API);
			glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);*/
	
			glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

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
			//long dpy = glfwGetEGLDisplay();
	
			try (MemoryStack stack = stackPush()) {
				IntBuffer major = stack.mallocInt(1);
				IntBuffer minor = stack.mallocInt(1);
	
/*				if (!eglInitialize(dpy, major, minor)) {
					throw new IllegalStateException(String.format("Failed to initialize EGL [0x%X]", eglGetError()));
				}
	
				m_egl = EGL.createDisplayCapabilities(dpy, major.get(0), minor.get(0));*/
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
			glfwSwapInterval(1);
//			m_gles = GLES.createCapabilities();
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
			GL.createCapabilities();
			
			System.out.println("GL_VENDOR: " + glGetString(GL_VENDOR));
			System.out.println("GL_VERSION: " + glGetString(GL_VERSION));
			System.out.println("GL_RENDERER: " + glGetString(GL_RENDERER));
	
			m_main_program = shaderInit("mainVertexShader.glsl", "mainFragmentShader.glsl");
			if (m_main_program == -1) {
				System.err.println("Failed to initialise main shaders");
//				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
			m_point_program = shaderInit("pointVertexShader.glsl", "pointFragmentShader.glsl");
			if (m_point_program == -1) {
				System.err.println("Failed to initialise point shaders");
//				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
			m_point_program_col = shaderInit("pointVertexShaderCol.glsl", "pointFragmentShaderCol.glsl");
			if (m_point_program_col == -1) {
				System.err.println("Failed to initialise colored point shaders");
//				GLES.setCapabilities(null);
	
				glfwFreeCallbacks(m_window);
				glfwTerminate();
				System.exit(1);
			}
		}
		
        GLDebugMessageCallback proc = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
        	if(severity == GL_DEBUG_SEVERITY_NOTIFICATION) return;
            System.err.println("[LWJGL] OpenGL debug message");
            printDetail(System.err, "ID", String.format("0x%X", id));
            printDetail(System.err, "Source", getDebugSource(source));
            printDetail(System.err, "Type", getDebugType(type));
            printDetail(System.err, "Severity", getDebugSeverity(severity));
            printDetail(System.err, "Message", GLDebugMessageCallback.getMessage(length, message));
        });
        
        glDebugMessageCallback(proc, NULL);		
        
        glEnable(GL_DEBUG_OUTPUT);

        makeFBOTexture(m_left_eye, l_hmd_width, l_hmd_height);
        if(m_left_eye.m_fbo == 0) System.exit(1);
        makeFBOTexture(m_right_eye, l_hmd_width, l_hmd_height);
        if(m_right_eye.m_fbo == 0) System.exit(1);
		glViewport(0, 0, l_hmd_width, l_hmd_height);
		if(!GLok("glViewport"))
		{
			return;
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
				TrackedDevicePose.Buffer tdpb = TrackedDevicePose.create(k_unMaxTrackedDeviceCount);
				TrackedDevicePose.Buffer tdpb2 = TrackedDevicePose.create(k_unMaxTrackedDeviceCount);
				 
				VRCompositor.VRCompositor_WaitGetPoses(tdpb, tdpb2);
				
				for(int i=0; i<VR.k_unMaxTrackedDeviceCount; ++i)
				{
/*					if(tdpb.get(i).bDeviceIsConnected())
					{
						System.out.println("Device "+i+" is of type "+VRSystem.VRSystem_GetTrackedDeviceClass(i));
					}*/
				}
				
				if(tdpb.get(VR.k_unTrackedDeviceIndex_Hmd).bPoseIsValid())
				{
					HmdMatrix34 l_headposition_matrix =  tdpb.get(VR.k_unTrackedDeviceIndex_Hmd).mDeviceToAbsoluteTracking();
					HmdMatrix34toMatrix4f(l_headposition_matrix, m_left_eye.m_headposition_matrix);
					m_left_eye.m_headposition_matrix.invert();
					m_right_eye.m_headposition_matrix.set(m_left_eye.m_headposition_matrix);
				}
				else
				{
					System.out.println("No head pos");
					m_left_eye.m_headposition_matrix = m_right_eye.m_headposition_matrix = new Matrix4f();
				}
				
				HmdMatrix44 l_mat44 = HmdMatrix44.create();
				HmdMatrix34 l_mat34 = HmdMatrix34.create();
				
				l_mat44 = VRSystem.VRSystem_GetProjectionMatrix(m_left_eye.m_eye, 1.0f, 30.0f, l_mat44);
				HmdMatrix44toMatrix4f(l_mat44, m_left_eye.m_projection_matrix);
				m_left_eye.m_projection_matrix.invert();
				l_mat34 = VRSystem.VRSystem_GetEyeToHeadTransform(m_left_eye.m_eye, l_mat34);
				HmdMatrix34toMatrix4f(l_mat34, m_left_eye.m_eyetohead_matrix);
				
				l_mat44 = VRSystem.VRSystem_GetProjectionMatrix(m_right_eye.m_eye, 1.0f, 30.0f, l_mat44);
				HmdMatrix44toMatrix4f(l_mat44, m_right_eye.m_projection_matrix);
				m_right_eye.m_projection_matrix.invert();
				l_mat34 = VRSystem.VRSystem_GetEyeToHeadTransform(m_right_eye.m_eye, l_mat34);
				HmdMatrix34toMatrix4f(l_mat34, m_right_eye.m_eyetohead_matrix);
				
			// Screen
				/*
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				render(m_left_eye);
				glfwSwapBuffers(m_window);
				*/
				
				glBindFramebuffer(GL_FRAMEBUFFER, m_left_eye.m_fbo);				
				render(m_left_eye);
				glBindFramebuffer(GL_FRAMEBUFFER, m_right_eye.m_fbo);				
				render(m_right_eye);
				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				
				Texture l_left_tx = Texture.create();
				Texture l_right_tx = Texture.create();
				l_left_tx.set(m_left_eye.m_fbo_texture, VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);
				l_right_tx.set(m_right_eye.m_fbo_texture, VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

				int l_ret = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, l_left_tx, null, VR.EVRSubmitFlags_Submit_Default);
				if(l_ret != 0) System.err.println("Left sub said: "+l_ret);
				l_ret = VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, l_right_tx, null, VR.EVRSubmitFlags_Submit_Default);
				if(l_ret != 0) System.err.println("Right sub said: "+l_ret);
				
//		}
		}
		glclear();
		VR.VR_ShutdownInternal();
	    glfwHideWindow(m_window);
		//GLES.setCapabilities(null);
		glfwFreeCallbacks(m_window);
		glfwTerminate();
		m_finished = true;
		System.exit(0);
	}

	
	public void glclear()
	{
    	glBindBuffer(GL_ARRAY_BUFFER, 0);
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    	if(m_intbuffer!=null) glDeleteBuffers(m_intbuffer);
	}

}
