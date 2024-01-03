package com.example.fingerprintrecogniction;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//import com.example.fingerprintrecogniction.ml.EncoderModel;
import com.example.fingerprintrecogniction.ml.Model;
import com.suprema.BioMiniFactory;
import com.suprema.CaptureResponder;
import com.suprema.IBioMiniDevice;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    private static BioMiniFactory mBioMiniFactory = null;
    public IBioMiniDevice mCurrentDevice = null;
    private int[] capturedGrayArray;


//    private List<Bitmap> users = new ArrayList<>();
    private float similarityThreshold = 0.999999f;
    private TextView debugTextView;
    private Button verifyButton;
    private Button captureButton;
    private TextView resultTextView ;
    private Bitmap img ;
    private List<TensorBuffer> users = new ArrayList<>();
    private int numBuffers = 0;
    private boolean matched = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = findViewById(R.id.CaptureSingle);
        verifyButton = findViewById(R.id.Verif);
        resultTextView = findViewById(R.id.resultText);
        debugTextView = findViewById(R.id.debugTextView);


        // Initialize BioMiniFactory
        mBioMiniFactory = new BioMiniFactory(this) {
            @Override
            public void onDeviceChange(DeviceChangeEvent event, Object dev) {
                if (event == DeviceChangeEvent.DEVICE_ATTACHED && mCurrentDevice == null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int cnt = 0;
                            while (mBioMiniFactory == null && cnt < 20) {
                                SystemClock.sleep(1000);
                                cnt++;
                            }
                            if (mBioMiniFactory != null) {
                                mCurrentDevice = mBioMiniFactory.getDevice(0);
                            }
                        }
                    }).start();
                } else if (mCurrentDevice != null && event == DeviceChangeEvent.DEVICE_DETACHED &&
                        mCurrentDevice.isEqual(dev)) {
                    Log.d(TAG, "mCurrentDevice removed: " + mCurrentDevice);
                    mCurrentDevice = null;
                }
            }
        };

        // Set button click listeners
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFingerprint();
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    Model model = Model.newInstance(getApplicationContext());
                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1,90,90,1}, DataType.FLOAT32);
                    int[] intValues = new int[90 * 90];
                    img.getPixels(intValues, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());
                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                    tensorImage.load(img);

                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*90*90);
                    byteBuffer.order(ByteOrder.nativeOrder());


                    int pixel = 0;
                    for(int i = 0; i < 90; i ++) {
                        for (int j = 0; j < 90; j++) {
                            int val = intValues[pixel++];
                            byteBuffer.putFloat((val) * (1.f / 255));
                        }
                    }
                    inputFeature0.loadBuffer(byteBuffer);

                    if(users.isEmpty()){users.add(inputFeature0);
                        numBuffers=1;}
                    else{
                        for (TensorBuffer inputFeature1 : users) {
                            Model.Outputs outputs = model.process(inputFeature0, inputFeature1);
                            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                            float[] queryVector =outputFeature0.getFloatArray();
                            debugTextView.setText(String.valueOf(queryVector[0]));
                            if(queryVector[0]==1.0){matched = true;resultTextView.setText("Matched");break;}
                        }
                        if(!matched){resultTextView.setText("Not Matched");users.add(inputFeature0);}

                    }
//                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }

            }
        });

    }
    private void captureFingerprint() {
        IBioMiniDevice.CaptureOption option = new IBioMiniDevice.CaptureOption();
        option.captureTemplate =false;

        mCurrentDevice.captureSingle(option,
                new CaptureResponder() {
                    @Override
                    public boolean onCaptureEx(final Object context, final Bitmap capturedImage,
                                               final IBioMiniDevice.TemplateData capturedTemplate,
                                               final IBioMiniDevice.FingerState fingerState) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView iv = findViewById(R.id.imagePreview);
                                if (iv != null) {
                                    iv.setImageBitmap(capturedImage);
                                    img=capturedImage;
                                    img = Bitmap.createScaledBitmap(img, 90, 90, false);
                                    img = convertToGrayscale(img);
                                }
                            }
                        });

                        debugTextView.append("\nCaptured fingerprint image");
                        return true;
                    }
                },
                false);
    }
    private float dotProduct(float[] a, float[] b) {
        float dotProduct = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
        }
        return dotProduct;
    }
    private Bitmap convertToGrayscale(Bitmap original) {
        Bitmap grayscaleBitmap = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // Converts the image to grayscale
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(original, 0, 0, paint);
        return grayscaleBitmap;
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (encoderInterpreter != null) {
//            encoderInterpreter.close();
//        }
//    }
}
