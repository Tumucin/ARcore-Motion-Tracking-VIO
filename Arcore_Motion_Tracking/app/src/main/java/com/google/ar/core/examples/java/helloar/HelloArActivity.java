/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {
  /** Static Values goes here */
  private static final String TAG = HelloArActivity.class.getSimpleName();
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;
  private static final int CUBEMAP_RESOLUTION = 16;
  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  /** Layout Desing parameters goes here */
  public TextView textViewTrackingState;
  public TextView textViewAccelerometer;
  public EditText editTextVIOARFileName;
  public TextView textViewFeaturesNumb;
  public TextView textViewSamplingRate;
  public EditText editTextIMUFileName;
  public GLSurfaceView surfaceView;
  public TextView textViewPose;
  public TextView textViewTime;

  /** Point Cloud */
  private VertexBuffer pointCloudVertexBuffer;
  private long lastPointCloudTimestamp = 0;
  public double numberOfFeaturePoints = 0;
  private Shader pointCloudShader;
  private Mesh pointCloudMesh;

  /** Arcore - VIO parameters */
  public double initialTimeStampVIOAr = 0;
  public double counterFrameVIOAr = 0;
  public double frameRateVIOAr = 0;
  public Frame currentFrameVIOAr;
  public double currentTimeVIOAr;

  /** Rendering Parameters goes here */
  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  public final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private final DepthSettings depthSettings = new DepthSettings();
  private DisplayRotationHelper displayRotationHelper;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;
  private PlaneRenderer planeRenderer;
  private boolean installRequested;
  private TapHelper tapHelper;
  private SampleRender render;
  public Snackbar snackbar;
  private Session session;

  /** Temporary matrix allocated here to reduce number of allocations for each frame.*/
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] projectionMatrix = new float[16];
  private final float[] viewMatrix = new float[16];

  private HELPERS helpers;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    helpers = new HELPERS(this);
    helpers.initializeLayout();
    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
    // Set up touch listener.
    tapHelper = new TapHelper(/*context=*/ this);
    surfaceView.setOnTouchListener(tapHelper);

    render = new SampleRender(surfaceView, this, getAssets());
    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
  }
  public void StartRecordingButtonClick(View view){
    /** When the button is pressed, then start recording accelerometer, gyroscope
     * and VIO values.
     * */

    helpers.startRecording(view); /**   Calls Sensor services*/
    helpers.getFileNamesFromEditText();
    helpers.showSnackMessageOnScreen(view);
  }
  public void RestartAppOnClick(View view){
    /** Restarts the app */
    finish();
    startActivity(getIntent());
  }
  public void onCheckBoxClicked(View view){
    /** Shows information on the screen if the check box is checked.*/
      boolean checked = ((CheckBox) view).isChecked();
      if (checked) {
          textViewPose.setVisibility(View.VISIBLE);
          textViewFeaturesNumb.setVisibility(View.VISIBLE);
          textViewSamplingRate.setVisibility(View.VISIBLE);
          textViewTime.setVisibility(View.VISIBLE);
          textViewTrackingState.setVisibility(View.VISIBLE);
          textViewAccelerometer.setVisibility(View.VISIBLE);
      }
      else{
          textViewPose.setVisibility(View.INVISIBLE);
          textViewFeaturesNumb.setVisibility(View.INVISIBLE);
          textViewSamplingRate.setVisibility(View.INVISIBLE);
          textViewTime.setVisibility(View.INVISIBLE);
          textViewTrackingState.setVisibility(View.INVISIBLE);
          textViewAccelerometer.setVisibility(View.INVISIBLE);
      }
  }
  public void SaveIMUVIOButtonClick(View view){
    /** Accelerometer, gyroscope and VIO are saved to a file when the button is pressed. */
    helpers.saveIMUVIOonClick(view);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }
        /** ARCore requires camera permissions to operate. If we did not yet obtain runtime
        permission on Android M and above, now is a good time to ask the user for it. */
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        /** Create the session. */
        session = new Session(/* context= */ this);
      } catch (UnavailableArcoreNotInstalledException
              | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    /** Note that order matters - see the note in onPause(), the reverse applies here. */
    try {
      configureSession();
      session.resume();
    } catch (CameraNotAvailableException e) {
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      session = null;
      return;
    }
    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    /** When the user is not using the app, then kills surfaceView and ARcore session.*/
    super.onPause();
    if (session != null) {
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    /** Some permission stuffs*/
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /*width=*/ 1, /*height=*/ 1);

      SpecularCubemapFilter cubemapFilter = new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      /*useMipmaps=*/
      // Environmental HDR
      Texture dfgTexture = new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /*useMipmaps=*/ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
              ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
              GLES30.GL_TEXTURE_2D,
              /*level=*/ 0,
              GLES30.GL_RG16F,
              /*width=*/ dfgResolution,
              /*height=*/ dfgResolution,
              /*border=*/ 0,
              GLES30.GL_RG,
              GLES30.GL_HALF_FLOAT,
              buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      // Point cloud
      pointCloudShader =
              Shader.createFromAssets(
                              render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", /*defines=*/ null)
                      .setVec4(
                              "u_Color", new float[] {100.0f / 255.0f, 0.0f / 255.0f, 0.0f / 255.0f, 1.0f})
                      .setFloat("u_PointSize", 15.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
              new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
              new Mesh(
                      render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers);

    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }

  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

  @Override
  public void onDrawFrame(SampleRender render) {
    counterFrameVIOAr++;
    if (session == null) {
      return;
    }

    if (!hasSetTextureNames) {
      session.setCameraTextureNames(
              new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
      hasSetTextureNames = true;
    }

    displayRotationHelper.updateSessionIfNeeded(session);
    try {
      currentFrameVIOAr = session.update();
    } catch (CameraNotAvailableException e) {
      Log.e(TAG, "Camera not available during onDrawFrame", e);
      messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
      return;
    }
    Camera camera = currentFrameVIOAr.getCamera();

    // Update BackgroundRenderer state to match the depth settings.
    try {
      backgroundRenderer.setUseDepthVisualization(
              render, depthSettings.depthColorVisualizationEnabled());
      backgroundRenderer.setUseOcclusion(render, depthSettings.useDepthForOcclusion());
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
      return;
    }
    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(currentFrameVIOAr);
    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());
    if (currentFrameVIOAr.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0);

    // Visualize tracked points.
    // Use try-with-resources to automatically release the point cloud.
    try (PointCloud pointCloud = currentFrameVIOAr.acquirePointCloud()) {
      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.getPoints());
        lastPointCloudTimestamp = pointCloud.getTimestamp();
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      render.draw(pointCloudMesh, pointCloudShader);
      helpers.calculateNumberOfFeaturePoints(pointCloud);
      helpers.updateInformationOnScreen();
    }
  }

  private void configureSession() {
    /** Configures the session with feature settings. */
    Config config = new Config(session);
    config.setFocusMode(Config.FocusMode.AUTO);
    session.configure(config);
  }
}
