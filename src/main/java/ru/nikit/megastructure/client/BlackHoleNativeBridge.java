package ru.nikit.megastructure.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import ru.nikit.megastructure.world.MegastructureChunkGenerator;

final class BlackHoleNativeBridge {
	private static final String LIBRARY_NAME = "megastructure_blackhole_bridge";
	private static final int TEXTURE_SIZE = 512;
	private static final String COMPOSITE_VERTEX_SHADER = """
			#version 150
			uniform mat4 ModelView;
			uniform mat4 Projection;
			uniform vec3 CoreRelative;
			uniform vec3 CameraRight;
			uniform vec3 CameraUp;
			uniform vec3 CameraRelative;
			uniform float Radius;
			uniform float Time;
			uniform float Inside;
			uniform float Fluctuation;
			uniform float Fall;
			uniform int Mode;
			in vec3 Position;
			in vec2 TexCoord;
			out vec3 vLocal;
			out vec2 vUv;
			out vec2 vNativeUv;
			out float vFacing;

			void main() {
				vec3 local = Position;
				float angle = atan(local.z, local.x);
				float radial = length(local.xz);
				float localRadius = length(local);
				if (Mode == 3 || Mode == 4) {
					float signY = Mode == 3 ? 1.0 : -1.0;
					vec2 toCamera = normalize(-CameraRelative.xz + vec2(0.0001, 0.0));
					vec2 diskPoint = normalize(local.xz + vec2(0.0001, 0.0));
					float rear = smoothstep(0.04, 0.94, -dot(diskPoint, toCamera));
					float diskWindow = smoothstep(1.35, 2.25, radial) * (1.0 - smoothstep(5.05, 5.65, radial));
					float shimmer = sin(Time * 0.045 + angle * 9.0 + radial * 1.7) * 0.012 * Fluctuation;
					local.xz *= 1.0 - rear * diskWindow * (0.18 + Fall * 0.06) + shimmer;
					local.y += signY * rear * diskWindow * (0.44 + Inside * 0.18);
					local.xz += vec2(-diskPoint.y, diskPoint.x) * rear * diskWindow * (0.08 + Inside * 0.06);
				} else if (Mode == 2 || Mode == 5) {
					float dragWave = sin(Time * 0.035 + angle * (2.0 + Fall * 3.0) + local.y * 4.5);
					float drag = (0.050 + Fluctuation * 0.025 + Inside * 0.12 + Fall * 0.18) * dragWave;
					local.xz += vec2(-local.z, local.x) * drag / max(radial, 0.3);
					local *= 1.0 + sin(Time * 0.041 + angle * 5.0 + local.y * 2.2) * (0.014 + Fall * 0.055) * Fluctuation;
					if (Mode == 5) {
						float tunnel = smoothstep(0.16, 1.0, localRadius);
						local.xz += vec2(-local.z, local.x) * tunnel * Fall * 0.22 / max(radial, 0.35);
						local.y += sin(Time * 0.052 + angle * 6.0) * tunnel * Fall * 0.13;
					}
				}
				vec3 relative = CoreRelative + local * Radius;
				gl_Position = Projection * ModelView * vec4(relative, 1.0);
				vec3 normal = normalize(local);
				vec3 view = normalize(CameraRelative);
				vLocal = local;
				vUv = TexCoord;
				vNativeUv = vec2(dot(normal, CameraRight) * 0.5 + 0.5, 0.5 - dot(normal, CameraUp) * 0.5);
				vFacing = clamp(dot(normal, view), 0.0, 1.0);
			}
			""";
	private static final String COMPOSITE_FRAGMENT_SHADER = """
			#version 150
			uniform sampler2D NativeFrame;
			uniform float Intensity;
			uniform float Time;
			uniform float Inside;
			uniform float Fluctuation;
			uniform float Fall;
			uniform int Mode;
			in vec3 vLocal;
			in vec2 vUv;
			in vec2 vNativeUv;
			in float vFacing;
			out vec4 fragColor;

			float ring(float value, float target, float width) {
				float d = (value - target) / max(width, 0.00001);
				return exp(-d * d);
			}

			void main() {
				vec2 sampleUv = clamp(vNativeUv, vec2(0.002), vec2(0.998));
				vec4 nativeColor = texture(NativeFrame, sampleUv);
				float radial = length(vLocal.xz);
				float localRadius = length(vLocal);
				float angle = atan(vLocal.z, vLocal.x);
				vec4 color = nativeColor;

				if (Mode == 0) {
					float rim = pow(1.0 - vFacing, 5.2);
					float photonPulse = 0.88 + 0.12 * sin(Time * 0.055 + angle * 11.0) * Fluctuation;
					float photon = ring(localRadius, 1.0, 0.035) * rim * photonPulse;
					float edgeHalo = ring(vFacing, 0.045, 0.030) * photonPulse;
					color.rgb = vec3(0.0, 0.0, 0.001)
							+ vec3(1.0, 0.68, 0.26) * photon * (0.38 + Intensity * 0.090 + Fall * 0.12)
							+ vec3(0.82, 0.52, 0.20) * edgeHalo * (0.020 + Fall * 0.025);
					color.a = 1.0;
				} else if (Mode == 1 || Mode == 3 || Mode == 4) {
					float disk = smoothstep(1.35, 1.62, radial) * (1.0 - smoothstep(5.1, 5.58, radial));
					float vertical = exp(-vLocal.y * vLocal.y * 130.0);
					float doppler = 0.24 + 0.76 * smoothstep(-0.68, 0.92, cos(angle + Time * 0.010));
					float filamentPulse = 0.88 + 0.12 * sin(angle * 13.0 + radial * 5.2 - Time * 0.050) * Fluctuation;
					float lane = 0.70 + 0.30 * sin(angle * 5.0 - log(max(radial, 1.001)) * 8.0 + Time * 0.032);
					float lensOrder = Mode == 1 ? 1.0 : (Mode == 3 ? 0.56 : 0.42);
					vec3 hot = mix(vec3(1.0, 0.23, 0.035), vec3(1.0, 0.82, 0.24), doppler);
					color.rgb = hot * disk * vertical * doppler * (0.42 + lane * 0.22) * lensOrder * filamentPulse;
					color.a = disk * vertical * (0.09 + doppler * 0.18) * Intensity * lensOrder * (0.93 + Fluctuation * 0.04);
				} else if (Mode == 5) {
					float rim = pow(1.0 - vFacing, 1.45);
					float spiral = ring(fract(angle / 6.2831853 * (3.0 + Fall * 2.0) + vLocal.y * 0.62 - Time * (0.030 + Fall * 0.030)), 0.5, 0.105);
					float throat = ring(localRadius, 1.0, 0.20 + Fall * 0.08);
					float shear = ring(abs(vLocal.y), 0.18 + sin(angle * 5.0 + Time * 0.044) * 0.050, 0.070);
					color.rgb = nativeColor.rgb * 0.28
							+ vec3(1.0, 0.56, 0.13) * spiral * 0.34
							+ vec3(0.22, 0.52, 1.0) * shear * 0.18
							+ vec3(0.02, 0.02, 0.035) * throat;
					color.a = (rim * 0.060 + spiral * 0.16 + shear * 0.080 + throat * 0.050) * Fall * Intensity;
				} else if (Mode == 6) {
					float edge = 1.0 - vFacing;
					float lens = ring(edge, 0.59 + Fall * 0.10, 0.24);
					float rearDrag = ring(edge, 0.88, 0.18) * (0.60 + Fall * 0.62);
					float photonHalo = ring(edge, 0.78 + Fall * 0.035, 0.085);
					float vortex = 0.5 + 0.5 * sin(angle * (6.0 + Fall * 5.0) - Time * (0.032 + Fall * 0.042) + vLocal.y * 4.8);
					float shearBands = ring(fract(angle / 6.2831853 * (4.0 + Fall * 3.0) + vLocal.y * 0.22 - Time * (0.018 + Fall * 0.032)), 0.5, 0.10);
					color.rgb = vec3(0.0, 0.0, 0.004)
							+ vec3(0.075, 0.16, 0.34) * lens * (0.54 + Fall * 0.44)
							+ vec3(1.0, 0.72, 0.27) * photonHalo * (0.20 + Fall * 0.12)
							+ vec3(0.95, 0.56, 0.18) * rearDrag * vortex * 0.095
							+ vec3(0.18, 0.38, 0.86) * shearBands * lens * (0.080 + Fall * 0.13);
					color.a = (lens * 0.27 + rearDrag * 0.22 + photonHalo * 0.18 + shearBands * lens * 0.075)
							* (0.72 + Fall * 0.88) * Intensity;
				} else {
					float rim = pow(1.0 - vFacing, 2.25);
					float caustic = ring(rim + nativeColor.a * 0.62, 0.86, 0.13);
					float shear = ring(abs(vLocal.y), 0.16 + sin(angle * 4.0 + Time * 0.030) * (0.035 + Fall * 0.055), 0.055 + Fall * 0.025);
					float pulse = 0.86 + 0.14 * sin(Time * 0.047 + angle * 8.0) * Fluctuation;
					color.rgb = nativeColor.rgb * 0.45 + vec3(0.95, 0.56, 0.20) * caustic * 0.26
							+ vec3(0.20, 0.48, 1.0) * shear * (0.12 + Fall * 0.16);
					color.a = (rim * 0.045 + caustic * 0.12 + shear * 0.055) * (0.75 + Inside * 0.75 + Fall * 0.55) * Intensity * pulse;
				}

				if (color.a < 0.004) {
					discard;
				}
				fragColor = color;
			}
			""";
	private static final boolean AVAILABLE = isNativeBridgeAllowed() && loadNativeLibrary();
	private static boolean failed;
	private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
	private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
	private static int nativeTexture;
	private static int compositeProgram;
	private static int compositeVertexShader;
	private static int compositeFragmentShader;
	private static int sphereVao;
	private static int sphereVbo;
	private static int sphereVertexCount;
	private static int diskVao;
	private static int diskVbo;
	private static int diskVertexCount;
	private static boolean compositeDisabled;

	private BlackHoleNativeBridge() {
	}

	static boolean render(
			WorldRenderContext context,
			Vec3d center,
			MegastructureChunkGenerator.BlackHoleCoreHint core,
			double time,
			float intensity,
			float fluctuation,
			float inside
	) {
		if (!AVAILABLE || failed) {
			return false;
		}
		if (!VulkanClientConfig.isVulkanEnabled()) {
			return false;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		Framebuffer framebuffer = client.getFramebuffer();
		if (framebuffer == null || framebuffer.fbo <= 0) {
			return false;
		}
		Vec3d camera = context.camera().getPos();
		float[] modelView = new float[16];
		float[] projection = new float[16];
		new Matrix4f(context.matrixStack().peek().getPositionMatrix()).get(modelView);
		new Matrix4f(RenderSystem.getProjectionMatrix()).get(projection);
		try {
			boolean rendered = render0(
					framebuffer.fbo,
					framebuffer.textureWidth,
					framebuffer.textureHeight,
					PIXELS,
					TEXTURE_SIZE,
					TEXTURE_SIZE,
					modelView,
					projection,
					camera.x,
					camera.y,
					camera.z,
					center.x,
					center.y,
					center.z,
					core.eventHorizonRadius(),
					core.influenceRadius(),
					core.seed(),
					time,
					intensity,
					fluctuation,
					inside
			);
			if (!rendered) {
				return false;
			}
			composeNativeFrame(context, center, core, time, intensity, fluctuation, inside);
			return !compositeDisabled;
		} catch (UnsatisfiedLinkError | RuntimeException error) {
			failed = true;
			System.err.println("Megastructure black-hole native bridge disabled after render failure: "
					+ error.getMessage());
			return false;
		}
	}

	static boolean renderVisualField(
			ByteBuffer pixels,
			int width,
			int height,
			long seed,
			double time,
			float intensity,
			int kind
	) {
		if (!AVAILABLE || failed || pixels == null || width <= 0 || height <= 0) {
			return false;
		}
		if (!VulkanClientConfig.isVulkanEnabled()) {
			return false;
		}
		try {
			return renderVisualField0(pixels, width, height, seed, time, intensity, kind);
		} catch (UnsatisfiedLinkError | RuntimeException error) {
			failed = true;
			System.err.println("Megastructure Vulkan visual-field bridge disabled after render failure: "
					+ error.getMessage());
			return false;
		}
	}

	private static void composeNativeFrame(
			WorldRenderContext context,
			Vec3d center,
			MegastructureChunkGenerator.BlackHoleCoreHint core,
			double time,
			float intensity,
			float fluctuation,
			float inside
	) {
		ensureCompositeGpu();
		if (compositeDisabled || compositeProgram == 0 || nativeTexture == 0 || sphereVao == 0 || diskVao == 0) {
			return;
		}

		boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
		int vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
		int currentProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
		int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		int unpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
		int unpackRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
		int unpackSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
		int unpackSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
		int blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
		int blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
		int blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
		int blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
		int cullFaceMode = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
		int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

		Vec3d camera = context.camera().getPos();
		Vec3d relative = center.subtract(camera);
		float fall = smoothstep(1.0F - clamp((float) (camera.distanceTo(center) / Math.max(core.eventHorizonRadius() * 10.0F, 1.0F)), 0.0F, 1.0F));
		Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(context.camera().getRotation());
		Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(context.camera().getRotation());
		Matrix4f modelView = new Matrix4f(context.matrixStack().peek().getPositionMatrix());
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());

		PIXELS.clear();
		RenderSystem.activeTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, nativeTexture);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
		GL11.glTexSubImage2D(
				GL11.GL_TEXTURE_2D,
				0,
				0,
				0,
				TEXTURE_SIZE,
				TEXTURE_SIZE,
				GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE,
				PIXELS
		);

		try {
			RenderSystem.enableDepthTest();
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.disableCull();
			RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
			RenderSystem.activeTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, nativeTexture);
			GL20.glUseProgram(compositeProgram);
			uploadMatrix(GL20.glGetUniformLocation(compositeProgram, "ModelView"), modelView);
			uploadMatrix(GL20.glGetUniformLocation(compositeProgram, "Projection"), projection);
			GL20.glUniform3f(
					GL20.glGetUniformLocation(compositeProgram, "CoreRelative"),
					(float) relative.x,
					(float) relative.y,
					(float) relative.z
			);
			GL20.glUniform3f(GL20.glGetUniformLocation(compositeProgram, "CameraRight"), right.x, right.y, right.z);
			GL20.glUniform3f(GL20.glGetUniformLocation(compositeProgram, "CameraUp"), up.x, up.y, up.z);
			GL20.glUniform3f(
					GL20.glGetUniformLocation(compositeProgram, "CameraRelative"),
					(float) -relative.x,
					(float) -relative.y,
					(float) -relative.z
			);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Intensity"), intensity);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Time"), (float) time);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Inside"), inside);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Fluctuation"), fluctuation);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Fall"), fall);
			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "NativeFrame"), 0);

			RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 3);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Radius"), core.eventHorizonRadius());
			GL30.glBindVertexArray(diskVao);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, diskVertexCount);

			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 4);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, diskVertexCount);

			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 1);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, diskVertexCount);

			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 6);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Radius"), core.eventHorizonRadius() * (5.8F + fall * 5.4F));
			GL30.glBindVertexArray(sphereVao);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, sphereVertexCount);

			if (fall > 0.03F) {
				RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
				GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 5);
				GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Radius"), core.eventHorizonRadius() * (2.05F + fall * 2.95F));
				GL30.glBindVertexArray(sphereVao);
				GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, sphereVertexCount);
			}

			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glCullFace(GL11.GL_BACK);
			GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "Mode"), 0);
			GL20.glUniform1f(GL20.glGetUniformLocation(compositeProgram, "Radius"), core.eventHorizonRadius() * 2.60F);
			GL30.glBindVertexArray(sphereVao);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, sphereVertexCount);
		} finally {
			GL20.glUseProgram(currentProgram);
			GL30.glBindVertexArray(vertexArray);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
			RenderSystem.activeTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, unpackAlignment);
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, unpackRowLength);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows);
			RenderSystem.activeTexture(activeTexture);
			GL11.glCullFace(cullFaceMode);
			GL11.glDepthFunc(depthFunc);
			RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
			setCapability(GL11.GL_BLEND, blendEnabled);
			setCapability(GL11.GL_DEPTH_TEST, depthEnabled);
			setCapability(GL11.GL_CULL_FACE, cullEnabled);
			RenderSystem.depthMask(depthWrite);
		}
	}

	private static void ensureCompositeGpu() {
		if (compositeProgram == 0) {
			compositeVertexShader = compile(GL20.GL_VERTEX_SHADER, COMPOSITE_VERTEX_SHADER);
			compositeFragmentShader = compile(GL20.GL_FRAGMENT_SHADER, COMPOSITE_FRAGMENT_SHADER);
			compositeProgram = GL20.glCreateProgram();
			GL20.glAttachShader(compositeProgram, compositeVertexShader);
			GL20.glAttachShader(compositeProgram, compositeFragmentShader);
			GL20.glBindAttribLocation(compositeProgram, 0, "Position");
			GL20.glBindAttribLocation(compositeProgram, 1, "TexCoord");
			GL20.glLinkProgram(compositeProgram);
			if (GL20.glGetProgrami(compositeProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				System.err.println("Megastructure black-hole native composite shader link failed: "
						+ GL20.glGetProgramInfoLog(compositeProgram));
				compositeDisabled = true;
			}
		}
		if (nativeTexture == 0) {
			int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
			int currentTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			nativeTexture = GL11.glGenTextures();
			RenderSystem.activeTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, nativeTexture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			PIXELS.clear();
			GL11.glTexImage2D(
					GL11.GL_TEXTURE_2D,
					0,
					GL11.GL_RGBA8,
					TEXTURE_SIZE,
					TEXTURE_SIZE,
					0,
					GL11.GL_RGBA,
					GL11.GL_UNSIGNED_BYTE,
					PIXELS
			);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
			RenderSystem.activeTexture(activeTexture);
		}
		if (sphereVao == 0) {
			Mesh sphere = createSphereMesh(42, 84);
			GpuMesh mesh = createMeshVao(sphere.data());
			sphereVao = mesh.vao();
			sphereVbo = mesh.vbo();
			sphereVertexCount = sphere.vertexCount();
		}
		if (diskVao == 0) {
			Mesh disk = createDiskMesh(256, 8);
			GpuMesh mesh = createMeshVao(disk.data());
			diskVao = mesh.vao();
			diskVbo = mesh.vbo();
			diskVertexCount = disk.vertexCount();
		}
	}

	private static GpuMesh createMeshVao(float[] data) {
		int vao = GL30.glGenVertexArrays();
		int vbo = GL15.glGenBuffers();
		GL30.glBindVertexArray(vao);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 20, 0L);
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 20, 12L);
		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		return new GpuMesh(vao, vbo);
	}

	private static Mesh createSphereMesh(int latitudeSteps, int longitudeSteps) {
		float[] data = new float[latitudeSteps * longitudeSteps * 6 * 5];
		int index = 0;
		for (int lat = 0; lat < latitudeSteps; lat++) {
			float v0 = lat / (float) latitudeSteps;
			float v1 = (lat + 1) / (float) latitudeSteps;
			float theta0 = (float) (-Math.PI * 0.5 + Math.PI * v0);
			float theta1 = (float) (-Math.PI * 0.5 + Math.PI * v1);
			for (int lon = 0; lon < longitudeSteps; lon++) {
				float u0 = lon / (float) longitudeSteps;
				float u1 = (lon + 1) / (float) longitudeSteps;
				float phi0 = (float) (Math.PI * 2.0 * u0);
				float phi1 = (float) (Math.PI * 2.0 * u1);
				index = putVertex(data, index, spherePoint(theta0, phi0), u0, v0);
				index = putVertex(data, index, spherePoint(theta1, phi0), u0, v1);
				index = putVertex(data, index, spherePoint(theta1, phi1), u1, v1);
				index = putVertex(data, index, spherePoint(theta0, phi0), u0, v0);
				index = putVertex(data, index, spherePoint(theta1, phi1), u1, v1);
				index = putVertex(data, index, spherePoint(theta0, phi1), u1, v0);
			}
		}
		return new Mesh(data, index / 5);
	}

	private static Mesh createDiskMesh(int segments, int rings) {
		float[] data = new float[segments * rings * 6 * 5];
		int index = 0;
		float inner = 1.35F;
		float outer = 5.55F;
		for (int ring = 0; ring < rings; ring++) {
			float r0 = inner + (outer - inner) * ring / rings;
			float r1 = inner + (outer - inner) * (ring + 1) / rings;
			float uv0 = ring / (float) rings;
			float uv1 = (ring + 1) / (float) rings;
			for (int segment = 0; segment < segments; segment++) {
				float u0 = segment / (float) segments;
				float u1 = (segment + 1) / (float) segments;
				float a0 = (float) (Math.PI * 2.0 * u0);
				float a1 = (float) (Math.PI * 2.0 * u1);
				index = putVertex(data, index, diskPoint(r0, a0), uv0, u0);
				index = putVertex(data, index, diskPoint(r1, a0), uv1, u0);
				index = putVertex(data, index, diskPoint(r1, a1), uv1, u1);
				index = putVertex(data, index, diskPoint(r0, a0), uv0, u0);
				index = putVertex(data, index, diskPoint(r1, a1), uv1, u1);
				index = putVertex(data, index, diskPoint(r0, a1), uv0, u1);
			}
		}
		return new Mesh(data, index / 5);
	}

	private static Vector3f spherePoint(float theta, float phi) {
		float cosTheta = (float) Math.cos(theta);
		return new Vector3f(
				cosTheta * (float) Math.cos(phi),
				(float) Math.sin(theta),
				cosTheta * (float) Math.sin(phi)
		);
	}

	private static Vector3f diskPoint(float radius, float angle) {
		return new Vector3f(radius * (float) Math.cos(angle), 0.0F, radius * (float) Math.sin(angle));
	}

	private static int putVertex(float[] data, int index, Vector3f position, float u, float v) {
		data[index++] = position.x;
		data[index++] = position.y;
		data[index++] = position.z;
		data[index++] = u;
		data[index++] = v;
		return index;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float smoothstep(float value) {
		float clamped = clamp(value, 0.0F, 1.0F);
		return clamped * clamped * (3.0F - 2.0F * clamped);
	}

	private static void uploadMatrix(int uniform, Matrix4f matrix) {
		MATRIX_BUFFER.clear();
		matrix.get(MATRIX_BUFFER);
		GL20.glUniformMatrix4fv(uniform, false, MATRIX_BUFFER);
	}

	private static int compile(int type, String source) {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.err.println("Megastructure black-hole native composite shader compile failed: "
					+ GL20.glGetShaderInfoLog(shader));
			compositeDisabled = true;
		}
		return shader;
	}

	private static void setCapability(int capability, boolean enabled) {
		if (capability == GL11.GL_BLEND) {
			if (enabled) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
		} else if (capability == GL11.GL_DEPTH_TEST) {
			if (enabled) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
		} else if (capability == GL11.GL_CULL_FACE) {
			if (enabled) RenderSystem.enableCull(); else RenderSystem.disableCull();
		}
	}

	private static boolean loadNativeLibrary() {
		String explicit = System.getProperty("megastructure.blackhole.native");
		if (explicit != null && !explicit.isBlank()) {
			return loadPath(Path.of(explicit));
		}
		Path local = Path.of("natives", mappedLibraryName());
		if (Files.isRegularFile(local) && loadPath(local)) {
			return true;
		}
		try {
			System.loadLibrary(LIBRARY_NAME);
			System.err.println("Megastructure black-hole native bridge loaded from java.library.path.");
			return true;
		} catch (UnsatisfiedLinkError ignored) {
			return false;
		}
	}

	private static boolean isNativeBridgeAllowed() {
		if (!VulkanClientConfig.isVulkanEnabled()) {
			System.err.println("Megastructure Vulkan bridge disabled by Imperfect_salvation client config.");
			return false;
		}
		String mode = System.getProperty("megastructure.vulkan", "auto").trim().toLowerCase(Locale.ROOT);
		if (mode.equals("off") || mode.equals("false") || mode.equals("0") || mode.equals("disabled")) {
			System.err.println("Megastructure Vulkan bridge disabled by megastructure.vulkan=" + mode + ".");
			return false;
		}
		if (mode.equals("force") || mode.equals("on") || mode.equals("true") || mode.equals("1")) {
			return true;
		}

		String vendor = glString(GL11.GL_VENDOR);
		String renderer = glString(GL11.GL_RENDERER);
		String gpu = (vendor + " " + renderer).toLowerCase(Locale.ROOT);
		if (isLegacyNonVulkanGpu(gpu)) {
			System.err.println("Megastructure Vulkan bridge disabled for legacy GPU: " + vendor + " / " + renderer);
			return false;
		}
		return true;
	}

	private static String glString(int name) {
		try {
			String value = GL11.glGetString(name);
			return value == null ? "" : value;
		} catch (RuntimeException error) {
			return "";
		}
	}

	private static boolean isLegacyNonVulkanGpu(String gpu) {
		return gpu.contains("radeon hd 2")
				|| gpu.contains("radeon hd 3")
				|| gpu.contains("radeon hd 4")
				|| gpu.contains("radeon hd 5")
				|| gpu.contains("radeon hd 6")
				|| gpu.contains("mobility radeon hd 2")
				|| gpu.contains("mobility radeon hd 3")
				|| gpu.contains("mobility radeon hd 4")
				|| gpu.contains("mobility radeon hd 5")
				|| gpu.contains("mobility radeon hd 6");
	}

	private static boolean loadPath(Path path) {
		try {
			System.load(path.toAbsolutePath().toString());
			System.err.println("Megastructure black-hole native bridge loaded: " + path.toAbsolutePath());
			return true;
		} catch (UnsatisfiedLinkError error) {
			System.err.println("Megastructure black-hole native bridge load failed: " + error.getMessage());
			return false;
		}
	}

	private static String mappedLibraryName() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("win")) {
			return LIBRARY_NAME + ".dll";
		}
		if (os.contains("mac")) {
			return "lib" + LIBRARY_NAME + ".dylib";
		}
		return "lib" + LIBRARY_NAME + ".so";
	}

	private record Mesh(float[] data, int vertexCount) {
	}

	private record GpuMesh(int vao, int vbo) {
	}

	private static native boolean render0(
			int framebuffer,
			int framebufferWidth,
			int framebufferHeight,
			ByteBuffer pixelBuffer,
			int pixelBufferWidth,
			int pixelBufferHeight,
			float[] modelView,
			float[] projection,
			double cameraX,
			double cameraY,
			double cameraZ,
			double coreX,
			double coreY,
			double coreZ,
			int eventHorizonRadius,
			int influenceRadius,
			long seed,
			double time,
			float intensity,
			float fluctuation,
			float inside
	);

	private static native boolean renderVisualField0(
			ByteBuffer pixelBuffer,
			int pixelBufferWidth,
			int pixelBufferHeight,
			long seed,
			double time,
			float intensity,
			int kind
	);
}
