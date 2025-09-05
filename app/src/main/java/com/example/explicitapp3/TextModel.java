package com.example.explicitapp3;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.TaskJniUtils;
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TextModel {
    private static final String TAG = "TextModel";
    BertNLClassifier classifier;
    private Context mcontext;
    private volatile boolean isInitialized = false;
    private CountDownLatch initLatch = new CountDownLatch(1);


    public void initTextModel(Context context, String textModelName) throws IOException {
        this.mcontext = context;
        BertNLClassifier.BertNLClassifierOptions options =
                BertNLClassifier.BertNLClassifierOptions.builder().build();

        ByteBuffer modelBuffer_base = TaskJniUtils.loadMappedFile(context, textModelName);
        classifier = BertNLClassifier.createFromBufferAndOptions(modelBuffer_base, options);

    }


    public void textRecognition(Bitmap bitmap) {
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, skipping text recognition");
            return;
        }

        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(mcontext).build();
            Frame frameimage = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlockSparseArray = textRecognizer.detect(frameimage);
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < textBlockSparseArray.size(); i++) {
                TextBlock textBlock = textBlockSparseArray.get(textBlockSparseArray.keyAt(i));
                text.append(" ").append(textBlock.getValue());
            }
            String ftext = text.toString().trim();
            Log.w(TAG, "textRecognition: TEXT IS: " + ftext);

            analyzeText(ftext);
            textRecognizer.release();

        } catch (Exception e) {
            Log.e(TAG, "Text recognition failed: " + e.getMessage());
        }
    }

    public void analyzeText(String text) {
        if (classifier == null) {
            Log.e(TAG, "Text classifier not initialized");
            return;
        }

        try {
            List<Category> results = classifier.classify(text);

            // usualy only the highest classification is needed like if its nsfw or not, but
            // there could be instance where it is both so we have to check their confidence score
            for (Category result : results) {
                String label = result.getLabel();
                float score = result.getScore();

                Log.i(TAG, "Text Classification - Label: " + label + ", Score: " + score);
            }

        } catch (Exception e) {
            Log.e(TAG, "Text analysis failed: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
    }
}