package p.purechords.tonegenerator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;
import java.util.Locale;

// Jsyn imports

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.SawtoothOscillatorBL;
import com.jsyn.unitgen.Select;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.SquareOscillatorBL;
import com.jsyn.unitgen.UnitVoice;
import com.jsyn.ports.UnitInputPort;
import com.jsyn.unitgen.SineOscillator;
import com.rey.material.widget.Slider;
import com.softsynth.shared.time.TimeStamp;
import com.jsyn.unitgen.PassThrough;
import com.jsyn.unitgen.WhiteNoise;
import com.jsyn.unitgen.Circuit;

public class MainActivity extends AppCompatActivity {

    // Declare objects and primitives

    private static LineOut lineOut;
    public static TestToneGenerator toneGenerator;

    float mainVolumeFloat = 0.35f;
    float freqData = 440.0f;

    int notesDataHarmonic = 45;
    int sourceFlag = 0;

    // Initialize synthesis engine

    Synthesizer synth = JSyn.createSynthesizer(new JSynAndroidAudioDevice());

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Create instance

        super.onCreate(savedInstanceState);

        // Load last user settings

        SharedPreferences sharedPref= getSharedPreferences("PureChordsTestToneGenPref", 0);
        if (sharedPref.contains("mainVolume")) {
            mainVolumeFloat = sharedPref.getFloat("mainVolume", mainVolumeFloat);
            freqData = sharedPref.getFloat("freqData", freqData);
            notesDataHarmonic = sharedPref.getInt("notesData", notesDataHarmonic);
            sourceFlag = sharedPref.getInt("sourceFlag", sourceFlag);
        }

        // Set view

        setContentView(R.layout.activity_main);

        // Force English

        Resources res = getApplicationContext().getResources();

        Locale locale = new Locale("en");
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.locale = locale;

        res.updateConfiguration(config, res.getDisplayMetrics());

        // Connect synth io

            synth.add(lineOut = new LineOut());
            synth.add(toneGenerator = new TestToneGenerator());
            toneGenerator.output.connect(0, lineOut.input, 0);
            toneGenerator.output.connect(0, lineOut.input, 1);

         // Define gui element listeners

        final ToggleButton toggleButtonPowerGet = findViewById(R.id.toggleButtonPower);
        toggleButtonPowerGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked()) {
                    if (!synth.isRunning()) {
                        synth.start();
                        lineOut.start();
                        toggleButtonPowerGet.setBackgroundColor(Color.parseColor("#FF3F51B5"));
                    }
                    if (synth.isRunning()) {
                        toggleButtonPowerGet.setBackgroundColor(Color.parseColor("#FF3F51B5"));
                    }
                }
                else {
                    lineOut.stop();
                    synth.stop();
                    toggleButtonPowerGet.setBackgroundColor(Color.parseColor("#9d9494"));
               //     powerFlag = 0;
                }
            }
        });

        final ToggleButton toggleButtonMuteGet = findViewById(R.id.toggleButtonMute);
        toggleButtonMuteGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked()) {
                    toggleButtonMuteGet.setBackgroundColor(Color.parseColor("#FF3F51B5"));
                    MuteAudio();
                }
                else {
                    toggleButtonMuteGet.setBackgroundColor(Color.parseColor("#9d9494"));
                    UnMuteAudio();
                }
            }
        });

        final Slider sliderMainVolumeGet = findViewById(R.id.sliderMainVolume);
        sliderMainVolumeGet.setValue(mainVolumeFloat * 100.0f, true);
        toneGenerator.amplitude.set(mainVolumeFloat);
        sliderMainVolumeGet.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider view, boolean fromUser, float oldPos, float newPos, int oldValue, int newValue) {
                mainVolumeFloat = (sliderMainVolumeGet.getValue() / 100.0f);
                toneGenerator.amplitude.set(mainVolumeFloat);
            }
        });

        final Spinner spinnerHarmonicNoteGet = (findViewById(R.id.spinnerHarmonicNote));
        spinnerHarmonicNoteGet.setOnItemSelectedListener(new MainActivity.CustomOnItemSelectedListenerHarmonicNote());
        ArrayAdapter adapterHarmonicNote = ArrayAdapter.createFromResource(this,
                R.array.spinnerNotes, R.layout.spinner_item);
        spinnerHarmonicNoteGet.setAdapter(adapterHarmonicNote);
        spinnerHarmonicNoteGet.setSelection(notesDataHarmonic);

        final Spinner spinnerSourceGet = (findViewById(R.id.spinnerSource));
        spinnerSourceGet.setOnItemSelectedListener(new MainActivity.CustomOnItemSelectedListenerSource());
        ArrayAdapter adapterSource = ArrayAdapter.createFromResource(this,
                R.array.spinnerSource, R.layout.spinner_item);
        spinnerSourceGet.setAdapter(adapterSource);
        spinnerSourceGet.setSelection(sourceFlag);

        final EditText freqFmGet = findViewById(R.id.editTextFreq);
        freqFmGet.setText(Float.toString(freqData));
        freqFmGet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (freqFmGet.getText().toString().equals("")) freqFmGet.setText("40");
                }
            }
        });

        freqFmGet.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String msgNullCheck = freqFmGet.getText().toString();
                if (!msgNullCheck.equals("")){
                    float msgGetCheckF = Float.parseFloat(freqFmGet.getText().toString());
                    if (msgGetCheckF == 0) {
                        freqFmGet.setText("");
                    }

                    if (msgGetCheckF > 22000) {
                        freqFmGet.setText("1000");
                    }

                    if ((msgGetCheckF != 0) && (msgGetCheckF <= 22000)) {
                        if (spinnerHarmonicNoteGet.getSelectedItemPosition() == 0) {
                            freqData = msgGetCheckF;
                            toneGenerator.frequency.set(freqData);
                        }

                        if (spinnerHarmonicNoteGet.getSelectedItemPosition() != 0) {
                            freqData = msgGetCheckF;
                        }
                    }
                }
            }
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

    }

    // Define Jsyn synthesis class, import from Syntona

    public class TestToneGenerator extends Circuit implements UnitVoice {
        // Declare units and ports.
        PassThrough mFrequencyPassThrough;
        public UnitInputPort frequency;
        PassThrough mAmplitudePassThrough;
        public UnitInputPort amplitude;
        PassThrough mOutputPassThrough;
        public UnitOutputPort output;
        WhiteNoise mWhiteNoise;
        Select mSelectSineSaw;
        SineOscillator mSineOsc;
        SawtoothOscillatorBL mSawOscBL;
        SquareOscillatorBL mSquareOscBL;
        Select mSelectSquareWhite;
        Select mSelectFinalSource;

        // Declare inner classes for any child circuits.

        public TestToneGenerator() {
            // Create unit generators.
            add(mFrequencyPassThrough = new PassThrough());
            addPort(frequency = mFrequencyPassThrough.input, "frequency");
            add(mAmplitudePassThrough = new PassThrough());
            addPort(amplitude = mAmplitudePassThrough.input, "amplitude");
            add(mOutputPassThrough = new PassThrough());
            addPort( output = mOutputPassThrough.output, "output");
            add(mWhiteNoise = new WhiteNoise());
            add(mSelectSineSaw = new Select());
            add(mSineOsc = new SineOscillator());
            add(mSawOscBL = new SawtoothOscillatorBL());
            add(mSquareOscBL = new SquareOscillatorBL());
            add(mSelectSquareWhite = new Select());
            add(mSelectFinalSource = new Select());
            // Connect units and ports.
            mFrequencyPassThrough.output.connect(mSineOsc.frequency);
            mFrequencyPassThrough.output.connect(mSawOscBL.frequency);
            mFrequencyPassThrough.output.connect(mSquareOscBL.frequency);
            mAmplitudePassThrough.output.connect(mWhiteNoise.amplitude);
            mAmplitudePassThrough.output.connect(mSineOsc.amplitude);
            mAmplitudePassThrough.output.connect(mSawOscBL.amplitude);
            mAmplitudePassThrough.output.connect(mSquareOscBL.amplitude);
            mWhiteNoise.output.connect(mSelectSquareWhite.inputB);
            mSelectSineSaw.output.connect(mSelectFinalSource.inputA);
            mSineOsc.output.connect(mSelectSineSaw.inputA);
            mSawOscBL.output.connect(mSelectSineSaw.inputB);
            mSquareOscBL.output.connect(mSelectSquareWhite.inputA);
            mSelectSquareWhite.output.connect(mSelectFinalSource.inputB);
            mSelectFinalSource.output.connect(mOutputPassThrough.input);
            // Setup
            frequency.setup(20.0, 440.0, 22000.0);
            amplitude.setup(0.0, 1.0, 1.0);
            mSelectSineSaw.select.set(1.0);
            mSelectSquareWhite.select.set(0.0);
            mSelectFinalSource.select.set(0.0);
        }

        public void noteOn(double frequency, double amplitude, TimeStamp timeStamp) {
            this.frequency.set(frequency, timeStamp);
            this.amplitude.set(amplitude, timeStamp);
        }

        public void noteOff(TimeStamp timeStamp) {
        }

        public UnitOutputPort getOutput() {
            return output;
        }
    }

    // Handle editText focus change

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    // Define spinner cases

    public class CustomOnItemSelectedListenerSource implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final EditText freqFmGet = findViewById(R.id.editTextFreq);
            switch (position) {
                case 0:
                    // Sine
                    toneGenerator.mSelectSineSaw.select.set(0);
                    toneGenerator.mSelectFinalSource.select.set(0);
                    toneGenerator.mSineOsc.setEnabled(true);
                    toneGenerator.mSawOscBL.setEnabled(false);
                    toneGenerator.mSquareOscBL.setEnabled(false);
                    toneGenerator.mWhiteNoise.setEnabled(false);
                    sourceFlag = 0;
                    break;
                case 1:
                    // Saw
                    toneGenerator.mSelectSineSaw.select.set(1);
                    toneGenerator.mSelectFinalSource.select.set(0);
                    toneGenerator.mSineOsc.setEnabled(false);
                    toneGenerator.mSawOscBL.setEnabled(true);
                    toneGenerator.mSquareOscBL.setEnabled(false);
                    toneGenerator.mWhiteNoise.setEnabled(false);
                    sourceFlag = 1;
                    break;
                case 2:
                    // Square
                    toneGenerator.mSelectSquareWhite.select.set(0);
                    toneGenerator.mSelectFinalSource.select.set(1);
                    toneGenerator.mSineOsc.setEnabled(false);
                    toneGenerator.mSawOscBL.setEnabled(false);
                    toneGenerator.mSquareOscBL.setEnabled(true);
                    toneGenerator.mWhiteNoise.setEnabled(false);
                    sourceFlag = 2;
                    break;
                case 3:
                    // White
                    toneGenerator.mSelectSquareWhite.select.set(1);
                    toneGenerator.mSelectFinalSource.select.set(1);
                    toneGenerator.mSineOsc.setEnabled(false);
                    toneGenerator.mSawOscBL.setEnabled(false);
                    toneGenerator.mSquareOscBL.setEnabled(false);
                    toneGenerator.mWhiteNoise.setEnabled(true);
                    sourceFlag = 3;
                    break;
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

    public class CustomOnItemSelectedListenerHarmonicNote implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final EditText freqFmGet = findViewById(R.id.editTextFreq);
            switch (position) {
                case 0:
                    // Freq
                    notesDataHarmonic = 0;
                    break;
                case 1:
                    // E0
                    freqData = (20.60f);
                    notesDataHarmonic = 1;
                    break;
                case 2:
                    // F0
                    freqData = (21.83f);
                    notesDataHarmonic = 2;
                    break;
                case 3:
                    // F#0
                    freqData = (23.12f);
                    notesDataHarmonic = 3;
                    break;
                case 4:
                    // G0
                    freqData = (24.50f);
                    notesDataHarmonic = 4;
                    break;
                case 5:
                    // G$0
                    freqData = (25.96f);
                    notesDataHarmonic = 5;
                    break;
                case 6:
                    // A0
                    freqData = (27.50f);
                    notesDataHarmonic = 6;
                    break;
                case 7:
                    // A#0
                    freqData = (29.14f);
                    notesDataHarmonic = 7;
                    break;
                case 8:
                    // B0
                    freqData = (30.85f);
                    notesDataHarmonic = 8;
                    break;
                case 9:
                    // C1
                    freqData = (32.70f);
                    notesDataHarmonic = 9;
                    break;
                case 10:
                    // C#1
                    freqData = (34.65f);
                    notesDataHarmonic = 10;
                    break;
                case 11:
                    // D1
                    freqData = (36.71f);
                    notesDataHarmonic = 11;
                    break;
                case 12:
                    // D#1
                    freqData = (38.89f);
                    notesDataHarmonic = 12;
                    break;
                case 13:
                    // E1
                    freqData = (41.20f);
                    notesDataHarmonic = 13;
                    break;
                case 14:
                    // F1
                    freqData = (43.65f);
                    notesDataHarmonic = 14;
                    break;
                case 15:
                    // F#1
                    freqData = (46.25f);
                    notesDataHarmonic = 15;
                    break;
                case 16:
                    // G1
                    freqData = (49.00f);
                    notesDataHarmonic = 16;
                    break;
                case 17:
                    // G#1
                    freqData = (51.91f);
                    notesDataHarmonic = 17;
                    break;
                case 18:
                    // A1
                    freqData = (55.00f);
                    notesDataHarmonic = 18;
                    break;
                case 19:
                    // A#1
                    freqData = (58.27f);
                    notesDataHarmonic = 19;
                    break;
                case 20:
                    // B1
                    freqData = (61.735f);
                    notesDataHarmonic = 20;
                    break;
                case 21:
                    // C2
                    freqData = (65.406f);
                    notesDataHarmonic = 21;
                    break;
                case 22:
                    // C#2
                    freqData = (69.296f);
                    notesDataHarmonic = 22;
                    break;
                case 23:
                    // D2
                    freqData = (73.416f);
                    notesDataHarmonic = 23;
                    break;
                case 24:
                    // D#2
                    freqData = (77.782f);
                    notesDataHarmonic = 24;
                    break;
                case 25:
                    // E2
                    freqData = (82.407f);
                    notesDataHarmonic = 25;
                    break;
                case 26:
                    // F2
                    freqData = (87.307f);
                    notesDataHarmonic = 26;
                    break;
                case 27:
                    // F#2
                    freqData = (92.499f);
                    notesDataHarmonic = 27;
                    break;
                case 28:
                    // G2
                    freqData = (97.999f);
                    notesDataHarmonic = 28;
                    break;
                case 29:
                    // G#2
                    freqData = (103.826f);
                    notesDataHarmonic = 29;
                    break;
                case 30:
                    // A2
                    freqData = (110.0f);
                    notesDataHarmonic = 30;
                    break;
                case 31:
                    // A#2
                    freqData = (116.541f);
                    notesDataHarmonic = 31;
                    break;
                case 32:
                    // B2
                    freqData = (123.471f);
                    notesDataHarmonic = 32;
                    break;
                case 33:
                    // C3
                    freqData = (130.813f);
                    notesDataHarmonic = 33;
                    break;
                case 34:
                    // C#3
                    freqData = (138.59f);
                    notesDataHarmonic = 34;
                    break;
                case 35:
                    // D3
                    freqData = (146.83f);
                    notesDataHarmonic = 35;
                    break;
                case 36:
                    // D#3
                    freqData = (155.56f);
                    notesDataHarmonic = 36;
                    break;
                case 37:
                    // E3
                    freqData = (164.81f);
                    notesDataHarmonic = 37;
                    break;
                case 38:
                    // F3
                    freqData = (174.61f);
                    notesDataHarmonic = 38;
                    break;
                case 39:
                    // F#3
                    freqData = (185.00f);
                    notesDataHarmonic = 39;
                    break;
                case 40:
                    // G3
                    freqData = (196.00f);
                    notesDataHarmonic = 40;
                    break;
                case 41:
                    // G#3
                    freqData = (207.65f);
                    notesDataHarmonic = 41;
                    break;
                case 42:
                    // A3
                    freqData = (220.00f);
                    notesDataHarmonic = 42;
                    break;
                case 43:
                    // A#3
                    freqData = (233.08f);
                    notesDataHarmonic = 43;
                    break;
                case 44:
                    // B3
                    freqData = (246.94f);
                    notesDataHarmonic = 44;
                    break;
                case 45:
                    // C4
                    freqData = (261.63f);
                    notesDataHarmonic = 45;
                    break;
                case 46:
                    // C#4
                    freqData = (277.18f);
                    notesDataHarmonic = 46;
                    break;
                case 47:
                    // D4
                    freqData = (293.66f);
                    notesDataHarmonic = 47;
                    break;
                case 48:
                    // D#4
                    freqData = (311.13f);
                    notesDataHarmonic = 48;
                    break;
                case 49:
                    // #4
                    freqData = (329.63f);
                    notesDataHarmonic = 49;
                    break;
                case 50:
                    // F4
                    freqData = (349.23f);
                    notesDataHarmonic = 50;
                    break;
                case 51:
                    // F#4
                    freqData = (369.99f);
                    notesDataHarmonic = 51;
                    break;
                case 52:
                    // G4
                    freqData = (392.00f);
                    notesDataHarmonic = 52;
                    break;
                case 53:
                    // G#4
                    freqData = (415.30f);
                    notesDataHarmonic = 53;
                    break;
                case 54:
                    // A4
                    freqData = (440.00f);
                    notesDataHarmonic = 54;
                    break;
                case 55:
                    // A#4
                    freqData = (466.16f);
                    notesDataHarmonic = 55;
                    break;
                case 56:
                    // B4
                    freqData = (493.88f);
                    notesDataHarmonic = 56;
                    break;
                case 57:
                    // C5
                    freqData = (523.25f);
                    notesDataHarmonic = 57;
                    break;
                case 58:
                    // C#5
                    freqData = (554.37f);
                    notesDataHarmonic = 58;
                    break;
                case 59:
                    // D5
                    freqData = (587.33f);
                    notesDataHarmonic = 59;
                    break;
                case 60:
                    // D#5
                    freqData = (622.25f);
                    notesDataHarmonic = 60;
                    break;
                case 61:
                    // E5
                    freqData = (659.25f);
                    notesDataHarmonic = 61;
                    break;
                case 62:
                    // F5
                    freqData = (698.46f);
                    notesDataHarmonic = 62;
                    break;
                case 63:
                    // F#5
                    freqData = (739.99f);
                    notesDataHarmonic = 63;
                    break;
                case 64:
                    // G5
                    freqData = (783.99f);
                    notesDataHarmonic = 64;
                    break;
                case 65:
                    // G#5
                    freqData = (830.61f);
                    notesDataHarmonic = 65;
                    break;
                case 66:
                    // A5
                    freqData = (880.00f);
                    notesDataHarmonic = 66;
                    break;
                case 67:
                    // A#5
                    freqData = (932.33f);
                    notesDataHarmonic = 67;
                    break;
                case 68:
                    // B5
                    freqData = (987.77f);
                    notesDataHarmonic = 68;
                    break;
                case 69:
                    // C6
                    freqData = (1046.50f);
                    notesDataHarmonic = 69;
                    break;
                case 70:
                    // C#6
                    freqData = (1108.73f);
                    notesDataHarmonic = 70;
                    break;
                case 71:
                    // D6
                    freqData = (1174.66f);
                    notesDataHarmonic = 71;
                    break;
                case 72:
                    // D#6
                    freqData = (1244.51f);
                    notesDataHarmonic = 72;
                    break;
                case 73:
                    // E6
                    freqData = (1318.51f);
                    notesDataHarmonic = 73;
                    break;
                case 74:
                    // F6
                    freqData = (1396.91f);
                    notesDataHarmonic = 74;
                    break;
                case 75:
                    // F#6
                    freqData = (1479.98f);
                    notesDataHarmonic = 75;
                    break;
                case 76:
                    // G6
                    freqData = (1567.98f);
                    notesDataHarmonic = 76;
                    break;
                case 77:
                    // G#6
                    freqData = (1661.22f);
                    notesDataHarmonic = 77;
                    break;
                case 78:
                    // A6
                    freqData = (1760.00f);
                    notesDataHarmonic = 78;
                    break;
                case 79:
                    // A#6
                    freqData = (1864.66f);
                    notesDataHarmonic = 79;
                    break;
                case 80:
                    // B6
                    freqData = (1975.53f);
                    notesDataHarmonic = 80;
                    break;
                case 81:
                    // C7
                    freqData = (2093.00f);
                    notesDataHarmonic = 81;
                    break;
                case 82:
                    // C#7
                    freqData = (2217.46f);
                    notesDataHarmonic = 82;
                    break;
                case 83:
                    // D7
                    freqData = (2349.32f);
                    notesDataHarmonic = 83;
                    break;
                case 84:
                    // D#7
                    freqData = (2489.02f);
                    notesDataHarmonic = 84;
                    break;
                case 85:
                    // E7
                    freqData = (2637.02f);
                    notesDataHarmonic = 85;
                    break;
                case 86:
                    // F7
                    freqData = (2793.83f);
                    notesDataHarmonic = 86;
                    break;
                case 87:
                    // F#7
                    freqData = (2959.96f);
                    notesDataHarmonic = 87;
                    break;
                case 88:
                    // G7
                    freqData = (3135.96f);
                    notesDataHarmonic = 88;
                    break;
                case 89:
                    // G#7
                    freqData = (3322.44f);
                    notesDataHarmonic = 89;
                    break;
                case 90:
                    // A7
                    freqData = (3520.00f);
                    notesDataHarmonic = 90;
                    break;
                case 91:
                    // A#7
                    freqData = (3729.31f);
                    notesDataHarmonic = 91;
                    break;
                case 92:
                    // B7
                    freqData = (3951.07f);
                    notesDataHarmonic = 92;
                    break;
                case 93:
                    // C8
                    freqData = (4186.01f);
                    notesDataHarmonic = 93;
                    break;
                case 94:
                    // C#8
                    freqData = (4434.92f);
                    notesDataHarmonic = 94;
                    break;
                case 95:
                    // D8
                    freqData = (4698.63f);
                    notesDataHarmonic = 95;
                    break;
                case 96:
                    // D#8
                    freqData = (4978.03f);
                    notesDataHarmonic = 96;
                    break;
                case 97:
                    // E8
                    freqData = (5274.04f);
                    notesDataHarmonic = 97;
                    break;
                case 98:
                    // F8
                    freqData = (5587.65f);
                    notesDataHarmonic = 98;
                    break;
                case 99:
                    // F#8
                    freqData = (5919.91f);
                    notesDataHarmonic = 99;
                    break;
                case 100:
                    // G8
                    freqData = (6271.93f);
                    notesDataHarmonic = 100;
                    break;
                case 101:
                    // G#8
                    freqData = (6644.88f);
                    notesDataHarmonic = 101;
                    break;
                case 102:
                    // A8
                    freqData = (7040.00f);
                    notesDataHarmonic = 102;
                    break;
                case 103:
                    // A#8
                    freqData = (7458.62f);
                    notesDataHarmonic = 103;
                    break;
                case 104:
                    // B8
                    freqData = (7902.13f);
                    notesDataHarmonic = 104;
                    break;
            }

           toneGenerator.frequency.set(freqData);
            freqFmGet.setText(Float.toString(freqData));

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

    // Define mute and unmute interruptions methods

    public void MuteAudio(){
        AudioManager mAlramMAnager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
            mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    public void UnMuteAudio(){
        AudioManager mAlramMAnager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
        } else {
            mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, false);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, false);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }
    }

    // Save current user settings on exit

    @Override
    protected void onPause() {
        super.onPause(); // Always call the superclass method first

        SharedPreferences sharedPref= getSharedPreferences("PureChordsTestToneGenPref", 0);
        SharedPreferences.Editor editor= sharedPref.edit();

        editor.putFloat("mainVolume", mainVolumeFloat);
        editor.putFloat("freqData", freqData);
        editor.putInt("notesData", notesDataHarmonic);
        editor.putInt("sourceFlag", sourceFlag);

        editor.apply();
    }

    // Stop the synth engine and unmute interruptions on exit

    @Override
    protected void onDestroy() {
        super.onDestroy(); // Always call the superclass method first
        lineOut.stop();
        synth.stop();
        UnMuteAudio();
    }
}
