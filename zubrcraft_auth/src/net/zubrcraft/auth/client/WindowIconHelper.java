package net.zubrcraft.auth.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class WindowIconHelper {
    private static final Logger LOGGER = LogManager.getLogger("ZubrCraftIcon");
    private static boolean applied = false;

    public static void applyIcon() {
        if (applied) return;
        try {
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null || mc.m_91268_() == null) return;
            long windowHandle = mc.m_91268_().m_85439_();
            if (windowHandle == 0L) return;

            String[] sizes = {"icon_16x16.png", "icon_32x32.png", "icon_48x48.png", "icon_128x128.png", "icon_256x256.png"};
            List<NativeImage> images = new ArrayList<>();
            for (String size : sizes) {
                InputStream is = WindowIconHelper.class.getResourceAsStream("/assets/minecraft/icons/" + size);
                if (is != null) {
                    try (is) {
                        images.add(NativeImage.m_85058_(is));
                    }
                }
            }

            if (images.isEmpty()) {
                LOGGER.warn("No ZubrCraft icon resources found!");
                return;
            }

            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            Class<?> glfwImageClass = Class.forName("org.lwjgl.glfw.GLFWImage");
            Class<?> bufferClass = Class.forName("org.lwjgl.glfw.GLFWImage$Buffer");
            Class<?> memoryUtilClass = Class.forName("org.lwjgl.system.MemoryUtil");

            Method memAlloc = memoryUtilClass.getMethod("memAlloc", int.class);
            Method memFree = memoryUtilClass.getMethod("memFree", ByteBuffer.class);

            Method mallocBuffer = glfwImageClass.getMethod("malloc", int.class);
            Object imageBuffer = mallocBuffer.invoke(null, images.size());

            Method position = bufferClass.getMethod("position", int.class);
            Method width = bufferClass.getMethod("width", int.class);
            Method height = bufferClass.getMethod("height", int.class);
            Method pixels = bufferClass.getMethod("pixels", ByteBuffer.class);

            List<ByteBuffer> allocatedBuffers = new ArrayList<>();

            for (int i = 0; i < images.size(); i++) {
                NativeImage img = images.get(i);
                int w = img.m_84982_();
                int h = img.m_85084_();
                ByteBuffer byteBuf = (ByteBuffer) memAlloc.invoke(null, w * h * 4);
                allocatedBuffers.add(byteBuf);

                IntBuffer intBuf = byteBuf.asIntBuffer();
                intBuf.put(img.m_266370_());

                position.invoke(imageBuffer, i);
                width.invoke(imageBuffer, w);
                height.invoke(imageBuffer, h);
                pixels.invoke(imageBuffer, byteBuf);

                img.close();
            }

            position.invoke(imageBuffer, 0);

            Method glfwSetWindowIcon = glfwClass.getMethod("glfwSetWindowIcon", long.class, bufferClass);
            glfwSetWindowIcon.invoke(null, windowHandle, imageBuffer);

            Method freeBuffer = bufferClass.getMethod("free");
            freeBuffer.invoke(imageBuffer);
            for (ByteBuffer b : allocatedBuffers) {
                memFree.invoke(null, b);
            }

            applied = true;
            LOGGER.info("Successfully applied official ZubrCraft window and taskbar icon!");
        } catch (Throwable t) {
            LOGGER.error("Failed to set window icon: " + t.getMessage(), t);
        }
    }
}
