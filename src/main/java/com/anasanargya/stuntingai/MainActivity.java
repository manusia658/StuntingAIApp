package com.anasanargya.stuntingai;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.InterpreterApi;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button predict;
    TextInputEditText umur, tb, bb;
    CheckBox lanang, wedok;
    TextView prediksi;
    String[] stuntinglist = {"Tall", "Stunted", "Normal", "Severely Stunted"};
    String[] wastinglist= {"Risk of Overweight", "Underweight", "Severely Underweight", "Normal weight"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        predict = findViewById(R.id.predict);
        umur = findViewById(R.id.umur);
        tb = findViewById(R.id.tb);
        bb = findViewById(R.id.bb);
        lanang = findViewById(R.id.lanang);
        wedok = findViewById(R.id.wedok);
        prediksi = findViewById(R.id.hasil);

        lanang.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    wedok.setChecked(false);
                }
            }
        });
        wedok.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    lanang.setChecked(false);
                }
            }
        });

        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!lanang.isChecked() && !wedok.isChecked()){
                    Toast.makeText(MainActivity.this, "Pilih Kelamin", Toast.LENGTH_LONG).show();
                } else if (umur.getText().toString().isEmpty()) {
                    umur.setError("Isi Terlebih Dahulu");
                } else if (tb.getText().toString().isEmpty()) {
                    tb.setError("Isi Terlebih Dahulu");
                } else if (bb.getText().toString().isEmpty()) {
                    bb.setError("Isi Terlebih Dahulu");
                } else {

                    float niumur = Float.parseFloat(umur.getText().toString()) / 100f;
                    float nitb = Float.parseFloat(tb.getText().toString()) / 100f;
                    float nibb = Float.parseFloat(bb.getText().toString()) / 100f;
                    float bibmi = nibb / (nitb * nitb);
                    System.out.println(niumur + " " + nitb + " " + nibb + " " + bibmi);
                    float[][] input = new float[1][5];
                    input[0] = new float[]{0.0f, niumur, nitb, nibb, bibmi};
                    float[][] outputA = new float[1][4];
                    float[][] outputB = new float[1][4];

                    Map<Integer, Object> outputs = new HashMap<>();
                    outputs.put(0, outputA);
                    outputs.put(1, outputB);

                    try {
                        Interpreter tfmodel = new Interpreter(loadmodelfile(getAssets(), "model.tflite"));
                        tfmodel.runForMultipleInputsOutputs(new Object[]{input}, outputs);

                        String stun = "";
                        String wast = "";

                        float[] rawA = outputA[0];
                        float[] rawB = outputB[0];
                        float beforenumber = Float.NEGATIVE_INFINITY;
                        int akurasi = 0;
                        for (int i = 0; i < rawA.length; i++) {
                            if (rawA[i] > beforenumber) {
                                beforenumber = rawA[i];
                                akurasi = Math.round(beforenumber*100);
                                stun = stuntinglist[i];
                            }
                        }
                        beforenumber = Float.NEGATIVE_INFINITY;
                        for (int i = 0; i < rawB.length; i++) {
                            if (rawB[i] > beforenumber) {
                                beforenumber = rawB[i];
                                wast = wastinglist[i];
                            }
                        }
                        prediksi.setText("Stunting: " + stun + "\n" + "Berat Badan: " + wast + "\n" + "Akurasi: " + akurasi + "%");

                    } catch (IOException e) {
                        System.out.println("error: " + e.getMessage());
                    }
                }
            }
        });
    }

    public MappedByteBuffer loadmodelfile(AssetManager assetManager, String modelfilename) throws IOException{
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelfilename);
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startofset = assetFileDescriptor.getStartOffset();
        long declarlenght = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startofset, declarlenght);
    }
}