package th.co.sic.app.nvtm.settings.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Locale;

import th.co.sic.app.nvtm.settings.MainActivity;
import th.co.sic.app.nvtm.settings.R;

public class ConfigFragment extends Fragment {

    final private static int[] intervals = new int[]{5, 10, 15, 30, 60};
    final private static int[] intervalsUnitIds = new int[]{R.string.seconds};

    final private static int[] wakeupTimes = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30};
    final private static int[] wakeupTimesUnitIds = new int[]{R.string.minutes};

    final private static int minThresholds = -37; /* Celsius */
    final private static int maxThresholds = 61; /* Celsius */
    final private static int[] thresholdUnitIds = new int[]{R.string.celsius};

    static private boolean enable = true; /* turn on/off */
    static private Calendar date = Calendar.getInstance(); /* date dd-MM-yyyy */
    static private Calendar time = Calendar.getInstance(); /* HH:mm */
    static private int interval; /* seconds */
    static private int wakeupTime; /* minutes */
    static private int startDelay = 0; /* seconds */
    static private int runningTime = 0; /* seconds */
    static private int upperThreshold = 37; /* deci-celsius */
    static private int lowerThreshold = -10; /* deci-celsius */
    static private int validMinimum = 0; /* deci-celsius */
    static private int validMaximum = 0; /* deci-celsius */

    static private int intervalNumber = 3;
    static private int wakeupTimeNumber = 1;

    static private int intervalUnit = 0;
    static private int wakeupTimeUnit = 0;
    static private int upperThresholdUnit = 0;
    static private int lowerThresholdUnit = 0;

    private static int[] upperThresholds() {
        int length = maxThresholds - lowerThreshold + 1;
        int[] values = new int[length];
        for (int i = 0; i < length; ++i) {
            values[i] = lowerThreshold + i;
        }
        return values;
    }

    private int upperThresholdNumber(int value) {
        return value - lowerThreshold;
    }

    private static int[] lowerThresholds() {
        int length = upperThreshold - minThresholds + 1;
        int[] values = new int[length];
        for (int i = 0; i < length; ++i) {
            values[i] = minThresholds + i;
        }
        return values;
    }

    private int lowerThresholdNumber(int value) {
        return value - minThresholds;
    }

    private void setEnable(boolean enable) {
        ConfigFragment.enable = enable;
    }

    private void setDate(Calendar date) {
        ConfigFragment.date = date;
    }

    private void setTime(Calendar time) {
        ConfigFragment.time = time;
    }

    static private void setInterval(int number, int unit) {
        ConfigFragment.intervalNumber = number;
        ConfigFragment.intervalUnit = unit;
        ConfigFragment.interval = intervals[number];
    }

    static private void setWakeupTime(int number, int unit) {
        ConfigFragment.wakeupTimeNumber = number;
        ConfigFragment.wakeupTimeUnit = unit;
        ConfigFragment.wakeupTime = wakeupTimes[number] * 60 /* minutes */;
    }

    static private void setUpperThreshold(int number, int unit) {
        ConfigFragment.upperThresholdUnit = unit;
        ConfigFragment.upperThreshold = upperThresholds()[number];
    }

    static private void setLowerThreshold(int number, int unit) {
        ConfigFragment.lowerThresholdUnit = unit;
        ConfigFragment.lowerThreshold = lowerThresholds()[number];
    }

    public void datePickerPopup(final View view) {
        final TextView dateTextView = view.findViewById(R.id.dateConfigTextView);

        Calendar date = Calendar.getInstance();
        if (dateTextView == view) {
            date = ConfigFragment.date;
        }

        final DatePickerDialog datePicker = new DatePickerDialog(view.getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                update(view, calendar);
            }
        }, date.get(Calendar.YEAR), time.get(Calendar.MONTH),  date.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    public void timePickerPopup(final View view) {
        final TextView timeTextView = view.findViewById(R.id.timeConfigTextView);

        Calendar time = Calendar.getInstance();
        if (timeTextView  == view) {
            time = ConfigFragment.time;
        }

        final TimePickerDialog timePicker = new TimePickerDialog(view.getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePickerView, int hourOfDay, int minute) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                update(view, calendar);
            }
        }, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), true);
        timePicker.show();
    }

    public void numberPickerPopup(final View view) {
        final TextView intervalTextView = view.findViewById(R.id.intervalConfigTextView);
        final TextView wakeupTimeTextView = view.findViewById(R.id.wakeupTimeConfigTextView);
        final TextView upperThresholdTextView = view.findViewById(R.id.upperThresholdConfigTextView);
        final TextView lowerThresholdTextView = view.findViewById(R.id.lowerThresholdConfigTextView);

        int[] values = null;
        int[] unitIds = null;
        int selectedValue = 0;
        int selectedUnit = 0;

        if (intervalTextView == view) {
            values = ConfigFragment.intervals;
            unitIds = ConfigFragment.intervalsUnitIds;
            selectedValue = ConfigFragment.intervalNumber;
            selectedUnit = ConfigFragment.intervalUnit;
        } else if (wakeupTimeTextView == view) {
             values = ConfigFragment.wakeupTimes;
             unitIds = ConfigFragment.wakeupTimesUnitIds;
             selectedValue = ConfigFragment.wakeupTimeNumber;
             selectedUnit = ConfigFragment.wakeupTimeUnit;
        } else if (upperThresholdTextView == view) {
             values = upperThresholds();
             unitIds = ConfigFragment.thresholdUnitIds;
             selectedValue = upperThresholdNumber(ConfigFragment.upperThreshold);
             selectedUnit = ConfigFragment.upperThresholdUnit;
        } else if (lowerThresholdTextView == view) {
             values = lowerThresholds();
             unitIds = ConfigFragment.thresholdUnitIds;
             selectedValue = lowerThresholdNumber(ConfigFragment.lowerThreshold);
             selectedUnit = ConfigFragment.lowerThresholdUnit;
        }

        LinearLayout linearLayout = new LinearLayout(view.getContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);

        final NumberPicker numberPicker;
        final NumberPicker unitPicker;
        if (values == null) {
            numberPicker = null;
        } else {
            String[] displayedValues = new String[values.length];
            for (int i = 0; i < displayedValues.length; i++) {
                displayedValues[i] = values[i] + " ";
            }

            numberPicker = new NumberPicker(view.getContext());
            numberPicker.setDisplayedValues(displayedValues);
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(values.length - 1);
            numberPicker.setValue(selectedValue);
            numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            numberPicker.setWrapSelectorWheel(false);
            numberPicker.setId(View.NO_ID);
            linearLayout.addView(numberPicker);
        }
        if (unitIds == null) {
            unitPicker = null;
        } else {
            String[] unitStrings = new String[unitIds.length];
            for (int i = 0; i < unitStrings.length; i++) {
                unitStrings[i] = getString(unitIds[i]) + " ";
            }
            unitPicker = new NumberPicker(view.getContext());
            unitPicker.setDisplayedValues(unitStrings);
            unitPicker.setMinValue(0);
            unitPicker.setMaxValue(unitIds.length - 1);
            unitPicker.setValue(selectedUnit);
            unitPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            unitPicker.setWrapSelectorWheel(false);
            unitPicker.setId(View.NO_ID);
            linearLayout.addView(unitPicker);
        }

        final AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
        alert.setCancelable(false);
        alert.setView(linearLayout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                int number = (numberPicker == null) ? 0 : numberPicker.getValue();
                int unit = (unitPicker == null) ? 0 : unitPicker.getValue();
                update(view, number, unit);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    /* -------------------------------------------------------------------------------- */

    public ConfigFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        connected(MainActivity.isConnected);
        View view = this.getView();
        if (view != null) {
            final Switch enableSwitch = view.findViewById(R.id.enableConfigSwitch);
            final TextView dateTextView = view.findViewById(R.id.dateConfigTextView);
            final TextView timeTextView = view.findViewById(R.id.timeConfigTextView);
            final TextView intervalTextView = view.findViewById(R.id.intervalConfigTextView);
            final TextView wakeupTimeTextView = view.findViewById(R.id.wakeupTimeConfigTextView);
            final TextView upperThresholdTextView = view.findViewById(R.id.upperThresholdConfigTextView);
            final TextView lowerThresholdTextView = view.findViewById(R.id.lowerThresholdConfigTextView);
            update(enableSwitch, ConfigFragment.enable);
            update(dateTextView, ConfigFragment.date);
            update(timeTextView, ConfigFragment.time);
            update(intervalTextView, ConfigFragment.intervalNumber, ConfigFragment.intervalUnit);
            update(wakeupTimeTextView, ConfigFragment.wakeupTimeNumber, ConfigFragment.wakeupTimeUnit);
            update(upperThresholdTextView, upperThresholdNumber(ConfigFragment.upperThreshold), ConfigFragment.upperThresholdUnit);
            update(lowerThresholdTextView, lowerThresholdNumber(ConfigFragment.lowerThreshold), ConfigFragment.lowerThresholdUnit);
        }
    }

    public void connected(boolean isConnected) {
        View view = this.getView();
        if (view != null) {
            Button button;
            button = view.findViewById(R.id.resetConfigButton);
            if (button != null) {
                button.setVisibility(isConnected ? View.VISIBLE : View.INVISIBLE);
            }
            button = view.findViewById(R.id.applyConfigButton);
            if (button != null) {
                button.setVisibility(isConnected ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    public void update(View changedView, int number, int unit) {
        View view = this.getView();
        if ((view != null) && (changedView != null)) {
            final TextView intervalTextView = view.findViewById(R.id.intervalConfigTextView);
            final TextView wakeupTimeTextView = view.findViewById(R.id.wakeupTimeConfigTextView);
            final TextView upperThresholdTextView = view.findViewById(R.id.upperThresholdConfigTextView);
            final TextView lowerThresholdTextView = view.findViewById(R.id.lowerThresholdConfigTextView);

            if (changedView == intervalTextView) {
                setInterval(number, unit);
                String s = getString(ConfigFragment.intervalsUnitIds[unit]);
                intervalTextView.setText(String.format(getString(R.string.interval_configRule), intervals[number], s));
            } else if (changedView == wakeupTimeTextView) {
                setWakeupTime(number, unit);
                String s = getString(ConfigFragment.wakeupTimesUnitIds[unit]);
                wakeupTimeTextView.setText(String.format(getString(R.string.wakeup_configRule), wakeupTimes[number], s));
            } else if ((changedView == upperThresholdTextView) || (changedView == lowerThresholdTextView)) {
                if (changedView == upperThresholdTextView) {
                    setUpperThreshold(number, unit);
                } else /* (changedView == lowerThresholdTextView) */ {
                    setLowerThreshold(number, unit);
                }
                String s;
                int n;
                s = getString(ConfigFragment.thresholdUnitIds[ConfigFragment.lowerThresholdUnit]);
                n = ConfigFragment.lowerThreshold;
                lowerThresholdTextView.setText(String.format(getString(R.string.lowThreshold_configRule), n, s));
                s = getString(ConfigFragment.thresholdUnitIds[ConfigFragment.upperThresholdUnit]);
                n = ConfigFragment.upperThreshold;
                upperThresholdTextView.setText(String.format(getString(R.string.highThreshold_configRule), n, s));
            }
        }
    }

    private void update(View changedView, Calendar calendar) {
        View view = this.getView();
        if ((view != null) && (changedView != null)) {
            final TextView dateTextView = view.findViewById(R.id.dateConfigTextView);
            final TextView timeTextView = view.findViewById(R.id.timeConfigTextView);

            if (changedView == dateTextView) {
                setDate(calendar);
                DateFormat dateFormat = SimpleDateFormat.getDateInstance();
                dateTextView.setText(String.format(getString(R.string.date_configRule), dateFormat.format(calendar.getTime())));
            } else if (changedView == timeTextView) {
                setTime(calendar);
                String hours = String.format(Locale.US, "%02d", calendar.get(Calendar.HOUR_OF_DAY));
                String minutes = String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE));
                String time = hours + ":" + minutes;
                timeTextView.setText(String.format(getString(R.string.time_configRule), time));
            }
        }
    }

    public void update(View changedView, boolean enable) {
        View view = this.getView();
        if ((view != null) && (changedView != null)) {
            final Switch enableSwitch = view.findViewById(R.id.enableConfigSwitch);

            if (changedView == enableSwitch) {
                setEnable(enable);
            }
        }
    }

    public static boolean getEnable() {
        return ConfigFragment.enable;
    }

    public static long getDateTime() {
        Calendar calendar = Calendar.getInstance();
        Calendar date = ConfigFragment.date;
        Calendar time = ConfigFragment.time;
        calendar.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), time.get(Calendar.HOUR), time.get(Calendar.MINUTE));
        return calendar.getTimeInMillis();
    }

    public static int getInterval() {
        return ConfigFragment.interval;
    }

    public static int getWakeupTime() {
        return ConfigFragment.wakeupTime;
    }

    public static int getStartDelay() {
        return ConfigFragment.startDelay;
    }

    public static int getRunningTime() {
        return ConfigFragment.runningTime;
    }

    public static int getUpperThreshold() {
        return ConfigFragment.upperThreshold;
    }

    public static int getLowerThreshold() {
        return ConfigFragment.lowerThreshold;
    }

    public static int getValidMinimum() {
        return ConfigFragment.validMinimum;
    }

    public static int getValidMaximum() {
        return ConfigFragment.validMaximum;
    }
}
