package org.robolectric.shadows;

import android.opengl.GLES20;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Fake implementation of {@link GLES20}
 */
@Implements(GLES20.class)
public class ShadowGLES20 {
  private static int textureCount = 0;
  private static int shaderCount = 0;
  private static int programCount = 0;

  @Implementation
  protected static void glGenTextures(int n, int[] textures, int offset) {
    for (int i = 0; i < n; i++) {
      textures[offset + i] = ++textureCount;
    }
  }

  @Implementation
  protected static int glCreateShader(int type) {
    if (type != GLES20.GL_VERTEX_SHADER && type != GLES20.GL_FRAGMENT_SHADER) {
      return GLES20.GL_INVALID_ENUM;
    }
    return ++shaderCount;
  }

  @Implementation
  protected static int glCreateProgram() {
    return ++programCount;
  }
}
