package org.openbase.bco.bcomfy;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.PointLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;

import java.util.ArrayList;
import java.util.List;

public class TangoRenderer extends Renderer {
    private static final String TAG = TangoRenderer.class.getSimpleName();

    // Rajawali texture used to render the Tango color camera.
    private ATexture tangoCameraTexture;
    private float[] textureCoords0 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};

    private ScreenQuad backgroundQuad;

    private List<Object3D> planes;
    private List<Object3D> spheres;

    // Keeps track of whether the scene camera has been configured.
    private boolean sceneCameraConfigured;

    public TangoRenderer(Context context) {
        super(context);
        planes = new ArrayList<>();
        spheres = new ArrayList<>();
    }

    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);

        if (backgroundQuad == null) {
            backgroundQuad = new ScreenQuad();
            backgroundQuad.getGeometry().setTextureCoords(textureCoords0);
        }
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        tangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(tangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(backgroundQuad, 0);

        PointLight pointLight = new PointLight();
        pointLight.setColor(Color.WHITE);
        pointLight.setPower(0.8f);
        pointLight.setPosition(0, 0, 0);
        getCurrentScene().addLight(pointLight);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {

    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * Update background texture's UV coordinates when device orientation is changed. i.e change
     * between landscape and portrait mode.
     * This must be run in the OpenGL thread.
     */
    public void updateColorCameraTextureUvGlThread(int rotation) {
        if (backgroundQuad == null) {
            backgroundQuad = new ScreenQuad();
        }

        float[] textureCoords =
                TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation);
        backgroundQuad.getGeometry().setTextureCoords(textureCoords, true);
        backgroundQuad.getGeometry().reload();
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        if (tangoCameraTexture == null) {
            return -1;
        }
        else {
            return tangoCameraTexture.getTextureId();
        }
    }

    public boolean isSceneCameraConfigured() {
        return sceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(float[] matrixFloats) {
        getCurrentCamera().setProjectionMatrix(new Matrix4(matrixFloats));
    }

    public void addGroundPlane(Vector3 position, Vector3 normal) {
        Material planeMaterial = new Material();
        planeMaterial.setColor(0x330000ff);
        planeMaterial.enableLighting(false);

        addPlane(position, normal, planeMaterial);
    }

    public void addCeilingPlane(Vector3 position, Vector3 normal) {
        Material planeMaterial = new Material();
        planeMaterial.setColor(0x33ff0000);
        planeMaterial.enableLighting(false);

        addPlane(position, normal, planeMaterial);
    }

    public void addWallPlane(Vector3 position, Vector3 normal) {
        Material planeMaterial = new Material();
        planeMaterial.setColor(0x3300ff00);
        planeMaterial.enableLighting(false);

        addPlane(position, normal, planeMaterial);
    }

    public void addPlane(Vector3 position, Vector3 normal, Material material) {
        Object3D planeObject = new Plane(0.5f, 0.5f, 10, 10);
        planeObject.setMaterial(material);
        planeObject.setDoubleSided(true);
        planeObject.setTransparent(true);
        planeObject.setPosition(position);
        planeObject.setLookAt(new Vector3().addAndSet(position, normal));

        planes.add(planeObject);

        getCurrentScene().addChild(planeObject);
    }

    public void addSphere(Vector3 vector3, int color) {
        Material sphereMaterial = new Material();
        sphereMaterial.setColor(color);
        sphereMaterial.enableLighting(true);
        sphereMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        sphereMaterial.setSpecularMethod(new SpecularMethod.Phong());

        Object3D sphereObject = new Sphere(0.1f, 20, 20);
        sphereObject.setMaterial(sphereMaterial);
        sphereObject.setPosition(vector3);

        spheres.add(sphereObject);

        getCurrentScene().addChild(sphereObject);
    }

    public void clearPlanes() {
        for (Object3D plane : planes) {
            getCurrentScene().removeChild(plane);
        }

        planes.clear();
    }

    public void clearSpheres() {
        for (Object3D sphere : spheres) {
            getCurrentScene().removeChild(sphere);
        }

        spheres.clear();
    }

    public void removeRecentPlane() {
        getCurrentScene().removeChild(planes.get(planes.size() - 1));

        planes.remove(planes.size() - 1);
    }
}
