package com.benli.autophoto;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 调用相机拍照
 * <p/>
 * Created by zhouninghua on 2015/11/9.
 */
public class CameraActivity extends Activity implements View.OnClickListener, SurfaceHolder.Callback {
    /**
     * 身份证正面拍照（真人展示的requestCode）
     */
    public static final int SHOW_POSITIVE_MODEL_REQUESTCODE = 4001;
    /**
     * 身份证正面拍照返回(请求拍照的requestcode)
     */
    public static final int TAKE_POSITIVE_PHOTO_REQUESTCODE = 4011;


    /**
     * 手持拍照（真人展示的requestCode）
     */
    public static final int SHOW_HOLD_IDCARD_MODEL_REQUESTCODE = 4003;

    /**
     * 手持拍照(请求拍照的requestcode)
     */
    public static final int TAKE_HOLD_IDCARD_PHOTO_REQUESTCODE = 4013;


    @BindView(R.id.camera_surfaceView)
    SurfaceView surfaceView;
    @BindView(R.id.camera_txt_enter)
    TextView txtEnter;
    @BindView(R.id.camera_img_takepicture)
    ImageView imgTakepicture;
    @BindView(R.id.camera_txt_cancel)
    TextView txtCancel;
    @BindView(R.id.camera_img_style)
    ImageView imgStyle;
    @BindView(R.id.camera_img_cameraTrans)
    ImageView imgCameraTrans;
    private int IS_TOOK = 0;//是否已经拍照 ,0为否
    private Camera mCamera;
    private Camera.Parameters parameters = null;
    private Bundle bundle = null;// 声明一个Bundle对象，用来存储数据
    private Intent intent;
    private boolean startFontCamera = true;
    private SurfaceHolder mHolder;
    private static final String DENY_CAMERA = "请在权限管理应用中允许星星钱袋访问你的相机";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        ButterKnife.bind(this);
        initView();
    }

    /**
     * 初始化界面方法
     */
    private void initView() {
        // step 1 初始化标题栏
        initTitleBar();

        // step 2 初始化数据
        initData();
        // step 3 数据绑定
        dataBind();
    }

    private void initTitleBar() {
//        new TitleBuilder(this, R.id.include_add_bank_card).withBackIcon().setMiddleTitleText("添加提现银行卡").withHomeIcon();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        intent = getIntent();
        mHolder = surfaceView.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.setKeepScreenOn(true);// 屏幕常亮
        mHolder.addCallback(this);// 为SurfaceView的句柄添加一个回调函数
        setImg(intent);
        txtEnter.setVisibility(View.INVISIBLE);
        txtCancel.setVisibility(View.INVISIBLE);
    }

    /**
     * 数据绑定
     */
    private void dataBind() {
        txtEnter.setOnClickListener(this);
        imgTakepicture.setOnClickListener(this);
        txtCancel.setOnClickListener(this);
        imgCameraTrans.setOnClickListener(this);
    }

    Thread startCameraThread;

    @Override
    protected void onResume() {
        super.onResume();
        if (this.checkCameraHardware(this) && (mCamera == null)) {
            startCameraThread = new Thread(startCameraRunnable);
            startCameraThread.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private Runnable startCameraRunnable = new Thread() {
        @Override
        public void run() {
            try {
                // 打开camera
                mCamera = getCamera();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mHolder != null) {
                            setStartPreview(mCamera, mHolder);
                        }
                    }
                });
            } catch (Exception e) {

            }
        }
    };

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            if (b){
                Toast.makeText(CameraActivity.this, "自动对焦完成,可以自动拍照啦", Toast.LENGTH_SHORT).show();
                try {
                    if (mCamera != null) {
                        mCamera.takePicture(null, null, new MyPictureCallback());
                        IS_TOOK = 1;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    autoFocusHandler.postDelayed(autoFocusRunnable, 1000);
                }
            }else {
                autoFocusHandler.postDelayed(autoFocusRunnable, 1000);
            }

            canFocused = true;
        }
    };

    Handler autoFocusHandler = new Handler();
    boolean canFocused = true;

    private Runnable autoFocusRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mCamera != null && canFocused){
                    mCamera.autoFocus(autoFocusCallback);
                }
            } catch (Exception e) {
                e.printStackTrace();
                autoFocusHandler.postDelayed(autoFocusRunnable, 1000);
            }

            canFocused = false;
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_img_takepicture:
                // 拍照
                if (IS_TOOK == 0) {
                    if (mCamera != null) {
                        mCamera.takePicture(null, null, new MyPictureCallback());
                        IS_TOOK = 1;
                    }
                }
                break;
            case R.id.camera_txt_enter:
                //  确认
                if (IS_TOOK == 1) {
                    if (bundle == null) {
                        Toast.makeText(getApplicationContext(), "bundle null",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            if (isHaveSDCard())
                                saveToSDCard(bundle.getByteArray("bytes"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();
                        imgTakepicture.setVisibility(View.INVISIBLE);
//                    txtCancel.setVisibility(View.INVISIBLE);
//                    txtEnter.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            case R.id.camera_txt_cancel:
                //  取消 和 重拍
                imgStyle.setVisibility(View.VISIBLE);
                setImg(intent);
                imgTakepicture.setVisibility(View.VISIBLE);
                if (IS_TOOK == 1) {
                    if (mCamera != null) {
                        IS_TOOK = 0;
                        setStartPreview(mCamera, mHolder);
                        txtEnter.setVisibility(View.INVISIBLE);
                        txtCancel.setVisibility(View.INVISIBLE);
                    }
                } else {
                    finish();
                }

                break;
            case R.id.camera_img_cameraTrans:
                CameraTrans(mHolder);
                break;
        }
    }

    /**
     * 设置camera显示取景画面,并预览
     *
     * @param camera
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            if (null != camera) {
                // 通过surfaceview显示取景画面
                parameters = camera.getParameters(); // 获取各项参数
                //矫正拍照之后图片的旋转的角度
                parameters.setRotation(0);
                Size previewSize = getBestSupportedSize(parameters
                        .getSupportedPreviewSizes());
                parameters.setPreviewSize(previewSize.width, previewSize.height);// 设置预览图片尺寸
                Size largestSize = getBestSupportedSize(parameters
                        .getSupportedPictureSizes());// 设置捕捉图片尺寸
                parameters.setPictureSize(largestSize.width, largestSize.height);
                if (startFontCamera) {
                    //连续对焦
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
                camera.setParameters(parameters);
                camera.setPreviewDisplay(holder);
                // 设置用于显示拍照影像的SurfaceHolder对象
                camera.setDisplayOrientation(getPreviewDegree(CameraActivity.this));
                camera.startPreview();
                camera.autoFocus(autoFocusCallback);
                camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。

                autoFocusHandler.postDelayed(autoFocusRunnable, 2000);
            } else {
                Toast.makeText(this, DENY_CAMERA, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    /**
     * 物理按键事件
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA: // 按下拍照按钮
                if (mCamera != null && event.getRepeatCount() == 0) {
                    // 拍照
                    //注：调用takePicture()方法进行拍照是传入了一个PictureCallback对象——当程序获取了拍照所得的图片数据之后
                    //，PictureCallback对象将会被回调，该对象可以负责对相片进行保存或传入网络
                    mCamera.takePicture(null, null, new MyPictureCallback());
                }
            case KeyEvent.KEYCODE_BACK:
                if (IS_TOOK == 0)
                    finish();
                else {
                    //  camera.startPreview();
                    imgTakepicture.setVisibility(View.VISIBLE);
                    txtCancel.performClick();
                    return false;
                }

                break;

        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 检验是否有SD卡
     *
     * @true or false
     */
    public static boolean isHaveSDCard() {
        return Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState());
    }

    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws IOException
     */
    public void saveToSDCard(byte[] data) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory()
                + BuildConfig.IMAGEPATH);
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录
            fileFolder.mkdirs();
        }
        byte[] buffer;
        buffer = data.clone();
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.flush();
        outputStream.write(buffer); // 写入sd卡中
        outputStream.close(); // 关闭输出流
        //2.为了可以在我们的图库里面直接看见新画的图片
        //因为安卓只有在sd卡插入和重新开机才会遍历sd卡，获得新的图片，所以这里模拟sd卡被插入
        //模拟一个sd卡被插入的事件
        //android4.4以上只有系统应用才可以ACTION_MEDIA_MOUNTED ，所以分情况
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            broadCastTheIntent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(Environment.getExternalStorageDirectory()));
        } else {
            broadCastTheIntent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(Environment.getExternalStorageDirectory()));
        }

//        setPictureDegreeZero(jpgFile.getAbsolutePath());
        //返回
        Intent intent = new Intent();
        intent.putExtra("ImgPath", Environment.getExternalStorageDirectory() + BuildConfig.IMAGEPATH + filename);
        setResult(40002, intent);
        finish();
    }

    private void setPictureDegreeZero(String path) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "no");
            exifInterface.saveAttributes();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void broadCastTheIntent(String action, Uri data) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setData(data);
        sendBroadcast(intent);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mHolder = surfaceHolder;
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }

        if (mCamera != null)
            setStartPreview(mCamera, surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
        //默认回复后置摄像头
        startFontCamera = true;
        mHolder = null;
    }

    private Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            camera = null;
        }
        return camera;
    }

    private void CameraTrans(SurfaceHolder mholder) {
        // 切换前后摄像头
        int cameraCount = 0;
        CameraInfo cameraInfo = new CameraInfo();
        cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
            if (startFontCamera) {
                // 现在是后置，变更为前置
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    /**
                     * 记得释放camera，方便其他应用调用
                     */
                    releaseCamera();
                    // 打开当前选中的摄像头
                    mCamera = Camera.open(i);
                    startFontCamera = false;
                    setStartPreview(mCamera, mholder);
                    break;
                }
            } else {
                // 现在是前置， 变更为后置
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    /**
                     * 记得释放camera，方便其他应用调用
                     */
                    releaseCamera();
                    mCamera = Camera.open(i);
                    startFontCamera = true;
                    setStartPreview(mCamera, mholder);
                    break;
                }
            }

        }
    }


    /**
     * 重构照相类
     *
     * @author
     */
    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                bundle = new Bundle();
                bundle.putByteArray("bytes", data); //将图片字节数据保存在bundle当中，实现数据交换
                imgTakepicture.setVisibility(View.INVISIBLE);
                txtCancel.setVisibility(View.VISIBLE);
//                txtEnter.setVisibility(View.VISIBLE);
                IS_TOOK = 1;
                mCamera.stopPreview();
                imgStyle.setVisibility(View.GONE);
                txtCancel.setText("重拍");
                txtEnter.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }


    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }

    private Size getBestSupportedSize(List<Size> sizes) {
        // 取能适用的最大的SIZE
        Size largestSize = sizes.get(0);
        int largestArea = sizes.get(0).height * sizes.get(0).width;
        for (Size s : sizes) {
            int area = s.width * s.height;
            if (area > largestArea) {
                largestArea = area;
                largestSize = s;
            }
        }
        return largestSize;
    }


    //设置提示图片
    private void setImg(Intent intent) {
        Log.d(getClass().getSimpleName(), "wcy+++ setImg intent" + intent.getIntExtra("type", 0));
        if (TAKE_POSITIVE_PHOTO_REQUESTCODE == intent.getIntExtra("type", 0)) {
            //正面
            imgStyle.setImageResource(R.mipmap.photo_idcard_model);
        } else if (TAKE_HOLD_IDCARD_PHOTO_REQUESTCODE == intent.getIntExtra("type", 0)) {
            //手持
            imgStyle.setImageResource(R.mipmap.photo_hold_idcard_model);
            imgStyle.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private DialogInterface.OnClickListener cameraExceptionListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            CameraActivity.this.finish();
        }
    };
}
