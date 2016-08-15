package org.hawkdev.apps.navigationforblind;

import android.content.Context;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * Created by nomo on 8/12/16.
 *
 * This class is reposible for handling both speech input and narrating to user
 */
public class SpeechProcessor {

    private final TextToSpeech textToSpeech;
//    private final SpeechRecognizer speechRecognizer;
    private Context context;


    public SpeechProcessor(final Context context){

        textToSpeech =new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);

                    narrateText(context.getString(R.string.welcome));
                    narrateText(context.getString(R.string.instructions));
                    narrateText(context.getString(R.string.post_instrucitons));
                }
            }
        });


//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
//        speechRecognizer.startListening(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    }

    public void narrateText(String text){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, this.hashCode() + "");
    }
}
