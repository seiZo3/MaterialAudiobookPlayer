package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.text.DecimalFormat;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.uitools.ThemeUtil;


public class AudioDialogFragment extends DialogFragment {
    public static final String TAG = AudioDialogFragment.class.getSimpleName();
    private static final String BOOK_ID = "BOOK_ID";
    private final DecimalFormat dbFormat = new DecimalFormat("0");

    public static AudioDialogFragment newInstance(long bookId) {
        AudioDialogFragment audioDialogFragment = new AudioDialogFragment();
        Bundle equalizerArgs = new Bundle();
        equalizerArgs.putLong(AudioDialogFragment.BOOK_ID, bookId);
        audioDialogFragment.setArguments(equalizerArgs);
        return audioDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TableLayout customView = new TableLayout(getActivity());
        customView.setOrientation(LinearLayout.VERTICAL);
        TableLayout.LayoutParams containerLayoutParams = new TableLayout.LayoutParams();
        containerLayoutParams.width = TableLayout.LayoutParams.MATCH_PARENT;
        containerLayoutParams.height = TableLayout.LayoutParams.MATCH_PARENT;
        customView.setColumnStretchable(1, true);
        customView.setLayoutParams(containerLayoutParams);

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final long bookId = getArguments().getLong(BOOK_ID);
        final Book book = db.getBook(bookId);
        if (book == null) {
            throw new IllegalArgumentException(TAG + " started without a valid book for bookId=" + bookId);
        }

        if (MediaPlayerController.playerCanSetSpeed) {
            View playbackItems = newItems();
            TextView capture = (TextView) playbackItems.findViewById(R.id.text1);
            customView.addView(playbackItems);

            DiscreteSeekBar speedBar = (DiscreteSeekBar) playbackItems.findViewById(R.id.seek1);
            final int internalMin = (int) (Book.MIN_SPEED * 100);
            final int internalMax = (int) (Book.MAX_SPEED * 100);
            speedBar.setProgress((int) (book.getPlaybackSpeed() * 100));
            speedBar.setMin(internalMin);
            speedBar.setMax(internalMax);
            final DecimalFormat speedFormat = new DecimalFormat("0.0");
            speedBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                @Override
                public int transform(int i) {
                    return i;
                }

                @Override
                public boolean useStringTransform() {
                    return true;
                }

                @Override
                public String transformToString(int value) {
                    float speed = value / 100F;
                    return speedFormat.format(speed) + "x";
                }
            });
            speedBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {

                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                    synchronized (db) {
                        Book book = db.getBook(bookId);
                        if (book != null) {
                            book.setPlaybackSpeed(discreteSeekBar.getProgress() / 100F);
                            db.updateBook(book);
                        }
                    }
                }
            });

            View description = newDescription(inflater);
            TextView left = (TextView) description.findViewById(R.id.des_left);
            TextView right = (TextView) description.findViewById(R.id.des_right);
            customView.addView(description);

            capture.setText(R.string.speed);
            left.setText("0.5x");
            right.setText("2.0x");
        }

        // loudness
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View playbackItems = newItems();
            TextView capture = (TextView) playbackItems.findViewById(R.id.text1);
            customView.addView(playbackItems);

            @SuppressLint("CutPasteId") DiscreteSeekBar loudnessBar =
                    (DiscreteSeekBar) playbackItems.findViewById(R.id.seek1);
            loudnessBar.setMax(Book.LOUDNESS_ENHANCED_MAX);
            loudnessBar.setProgress(book.getLoudnessEnhanced());
            loudnessBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                @Override
                public int transform(int i) {
                    return i;
                }

                @Override
                public String transformToString(int value) {
                    return (value / 100) + " dB";
                }

                @Override
                public boolean useStringTransform() {
                    return true;
                }
            });
            loudnessBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                @Override
                public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int i, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {

                }

                @Override
                public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
                    synchronized (db) {
                        Book book = db.getBook(bookId);
                        if (book != null) {
                            book.setLoudnessEnhanced(discreteSeekBar.getProgress());
                            db.updateBook(book);
                        }
                    }
                }
            });


            View description = newDescription(inflater);
            TextView left = (TextView) description.findViewById(R.id.des_left);
            TextView right = (TextView) description.findViewById(R.id.des_right);
            customView.addView(description);


            capture.setText(getString(R.string.loudness) + "+");
            left.setText("0 dB");
            right.setText((Book.LOUDNESS_ENHANCED_MAX / 100) + " dB");
        }

        Equalizer equalizer = new Equalizer(0, 500);
        try {
            final short minEQLevel = equalizer.getBandLevelRange()[0];
            final short maxEQLevel = equalizer.getBandLevelRange()[1];
            for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                final short band = i;

                // init views
                View tableRow = newItems();
                customView.addView(tableRow);
                TextView frequencyView = (TextView) tableRow.findViewById(R.id.text1);
                DiscreteSeekBar equalizerBar = (DiscreteSeekBar) tableRow.findViewById(R.id.seek1);

                // set text
                frequencyView.setText((equalizer.getCenterFreq(band) / 1000) + " Hz");

                // set seekBar
                equalizerBar.setMin(minEQLevel);
                equalizerBar.setMax(maxEQLevel);
                equalizerBar.setNumericTransformer(new DiscreteSeekBar.NumericTransformer() {
                    @Override
                    public int transform(int i) {
                        return i;
                    }

                    @Override
                    public String transformToString(int value) {
                        return formatMilliBelToDb(value);
                    }

                    @Override
                    public boolean useStringTransform() {
                        return true;
                    }
                });
                short level = book.getBandLevel(band);
                if (level == -1) { // if band has not been set yet, set default band
                    level = equalizer.getBandLevel(band);
                }
                equalizerBar.setProgress(level - minEQLevel);
                equalizerBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
                    @Override
                    public void onProgressChanged(DiscreteSeekBar seekBar, int progress, boolean fromUser) {
                    }

                    @Override
                    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                        final short level = (short) (seekBar.getProgress() + minEQLevel);
                        synchronized (db) {
                            Book book = db.getBook(bookId);
                            if (book != null) {
                                book.setBandLevel(band, level);
                                db.updateBook(book);
                            }
                        }
                    }
                });


                View description = newDescription(inflater);
                customView.addView(description);
                TextView left = (TextView) description.findViewById(R.id.des_left);
                TextView right = (TextView) description.findViewById(R.id.des_right);

                left.setText(formatMilliBelToDb(minEQLevel));
                right.setText(formatMilliBelToDb(maxEQLevel));
            }
        } finally {
            equalizer.setEnabled(false);
            equalizer.release();
        }


        return new MaterialDialog.Builder(getActivity())
                .title(R.string.audio_settings_title)
                .customView(customView, true)
                .build();
    }

    private String formatMilliBelToDb(int level) {
        return dbFormat.format(level / 100.0F) + " dB";
    }

    private View newDescription(LayoutInflater inflater) {
        return inflater.inflate(R.layout.dialog_sound, null, false);
    }

    private View newItems() {
        // init row
        TableRow tableRow = new TableRow(getActivity());
        TableRow.LayoutParams tableRowLayoutParams = new TableRow.LayoutParams();
        tableRow.setLayoutParams(tableRowLayoutParams);

        // init text
        TextView frequencyView = new TextView(getActivity());
        frequencyView.setId(R.id.text1);
        frequencyView.setTextColor(getResources().getColor(ThemeUtil.getResourceId(getActivity(),
                R.attr.text_secondary)));
        frequencyView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension
                (R.dimen.list_text_secondary_size));
        TableRow.LayoutParams frequencyLayoutParams = new TableRow.LayoutParams();
        frequencyLayoutParams.gravity = Gravity.CENTER_VERTICAL;
        frequencyView.setLayoutParams(frequencyLayoutParams);
        tableRow.addView(frequencyView);

        // init seekBar
        DiscreteSeekBar seekBar = new DiscreteSeekBar(getActivity());
        seekBar.setId(R.id.seek1);
        TableRow.LayoutParams seekBarLayoutParams = new TableRow.LayoutParams();
        seekBarLayoutParams.weight = 1;
        seekBar.setLayoutParams(seekBarLayoutParams);
        tableRow.addView(seekBar);

        return tableRow;
    }
}
