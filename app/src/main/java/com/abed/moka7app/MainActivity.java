package com.abed.moka7app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

//·······················pbz·························//
    private Button pingButton, addVariablesButton;
    private EditText IpAddressEditText;
    private TextView appWifiIpViewText,rackViewText,slotViewText;
    private Spinner rackSpinner,slotSpinner;
    private RadioGroup radioGroupOfButtons;
    private RadioButton s7_1500RadioButton;
    private ImageView pingStatusImageView;
    private ProgressBar pingProgressBar;
    private ConstraintLayout mainConstraintLayout;
    private int variable=0;




    //handler for ping status
    private Handler pingUpdater =new Handler();


//···················································//




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();


        //ip filter restriction
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        IpAddressEditText.setFilters(filters);
        // un/focus for editedIP
        IpAddressEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    //Log.d("focus", "focus lost");
                    // Do whatever you want here
                } else {
                    //Log.d("focus", "focused");
                }

            }
        });

        IpAddressEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {


                return false;
            }
        });


                //hide add variables button in case of ip address has been changed
        IpAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (addVariablesButton.getVisibility() == View.VISIBLE)
                {
                    addVariablesButton.setVisibility(View.GONE);
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });



        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //createSpinner();
                //openSecondActivity();
                //disable ping button
                pingButton.setEnabled(false);
                //disable ip address object
                IpAddressEditText.setEnabled(false);

                pingButton.setVisibility(View.GONE);
                pingProgressBar.setVisibility(View.VISIBLE);
                pingStatusImageView.setVisibility(View.GONE);
                //check assigned number for this object -> 2131230833
                //Toast.makeText(MainActivity.this, String.valueOf(radioGroupOfButtons.getCheckedRadioButtonId()), Toast.LENGTH_LONG).show();
                checkWifiIp();
                pingPlc();
            }
        });



        radioGroupOfButtons.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {

                if (checkedId== R.id.s7_400RadioButton || checkedId== R.id.winAcRadioButton)
                {
                    rackViewText.setVisibility(View.VISIBLE);
                    slotViewText.setVisibility(View.VISIBLE);
                    rackSpinner.setVisibility(View.VISIBLE);
                    slotSpinner.setVisibility(View.VISIBLE);
                }
                else
                {
                    rackViewText.setVisibility(View.GONE);
                    slotViewText.setVisibility(View.GONE);
                    rackSpinner.setVisibility(View.GONE);
                    slotSpinner.setVisibility(View.GONE);
                    rackSpinner.setSelection(0);
                    slotSpinner.setSelection(0);

                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(notifyTextReceiver,
                new IntentFilter("notifyText"));
            }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    //Log.d("focus", "touchevent");
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }


    private void pingPlc() {
        new Thread()
        {
            @Override
            public void run() {
                Runtime runtime = Runtime.getRuntime();
                try
                {   String command="/system/bin/ping -c 1 "+ IpAddressEditText.getText().toString();
                    Process  mIpAddrProcess = runtime.exec(command);
                    final int mExitValue = mIpAddrProcess.waitFor();

                    pingUpdater.post((new Runnable() {
                        @Override
                        public void run() {
                            if(mExitValue==0){
                                pingStatusImageView.setImageResource(R.drawable.ic_checked);
                                //plc accessible
                                addVariablesButton.setVisibility(View.VISIBLE);
                            }else{
                                pingStatusImageView.setImageResource(R.drawable.ic_unchecked);
                            }

                            pingProgressBar.setVisibility(View.GONE);
                            pingStatusImageView.setVisibility(View.VISIBLE);
                            final Handler handler=new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    pingStatusImageView.setVisibility(View.GONE);
                                    //enable again ping button
                                    pingButton.setEnabled(true);
                                    //enable again ip address object
                                    IpAddressEditText.setEnabled(true);

                                    pingButton.setVisibility(View.VISIBLE);

                                }
                            },5000);
                        }
                    }));

                    return;
                }
                catch (InterruptedException ignore)
                {
                    ignore.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                return;

            }
        }.start();

    }

    private void createSpinner()
    {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,getResources().getStringArray(R.array.S7_area));
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        Toast.makeText(MainActivity.this, "create spinner", Toast.LENGTH_LONG).show();
    }

    private void initViews() {
        //button
        pingButton=findViewById(R.id.pingButton);
        addVariablesButton =findViewById(R.id.addVariablesButton);

        //editText
        IpAddressEditText=findViewById(R.id.IpAddressEditText);
        //textView
        appWifiIpViewText=findViewById(R.id.appWifiIpViewText);
        rackViewText=findViewById(R.id.rackViewText);
        slotViewText=findViewById(R.id.slotViewText);
        //spinner
        rackSpinner=findViewById(R.id.rackSpinner);
        slotSpinner=findViewById(R.id.slotSpinner);
        //radioGroup
        radioGroupOfButtons=findViewById(R.id.radioGroupOfButtons);
        //default Radio Button
        s7_1500RadioButton=findViewById(R.id.s7_1500RadioButton);
        //imageView
        pingStatusImageView=findViewById(R.id.pingStatusImageView);
        //progressBar
        pingProgressBar=findViewById(R.id.pingProgressBar);
        //layout
        mainConstraintLayout=findViewById(R.id.mainConstraintLayout);
    }

    private void checkWifiIp()
    {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        appWifiIpViewText.setText(Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()));
    }

    public void openSecondActivity(View v)
    {
        //jump to another activity
        Intent jumpIntent = new Intent(this, SecondActivity.class);
        startActivity(jumpIntent);

        //pass data to another activity
        Intent dataIntent = new Intent(this, SecondActivity.class);
        dataIntent.putExtra("ip", IpAddressEditText.getText().toString());
        dataIntent.putExtra("rack", rackSpinner.getSelectedItemPosition());
        dataIntent.putExtra("slot", slotSpinner.getSelectedItemPosition());
        startActivity(dataIntent);
    }
    @Override
    protected void onStart() {super.onStart();}

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences connectionParameters=getSharedPreferences("connectionParameters",Context.MODE_PRIVATE);
        //local ipAddress
        String ipAddress = connectionParameters.getString("spIpAddress", "");
        IpAddressEditText.setText(ipAddress);
        int a = connectionParameters.getInt("spCpuModel", s7_1500RadioButton.getId());
        radioGroupOfButtons.check(a);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {super.onRestart();}

    @Override
    protected void onDestroy() {super.onDestroy();}

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences connectionParameters=getSharedPreferences("connectionParameters",Context.MODE_PRIVATE);
        SharedPreferences.Editor spEdited=connectionParameters.edit();
        spEdited.putString("spIpAddress",IpAddressEditText.getText().toString());
        spEdited.putInt("spCpuModel",radioGroupOfButtons.getCheckedRadioButtonId());

        //Integer.parseInt(String.valueOf(radioGroupOfButtons.getCheckedRadioButtonId()))
        //((RadioButton)radioGroupOfButtons.getChildAt(index)).setChecked(true);
        //Toast.makeText(MainActivity.this, String.valueOf(radioGroupOfButtons.getCheckedRadioButtonId()), Toast.LENGTH_SHORT).show();

        spEdited.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_LONG).show();
        } else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    private BroadcastReceiver notifyTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast t = Toast.makeText(getApplicationContext(), intent.getStringExtra("Message"), Toast.LENGTH_LONG);
            t.show();
        }
    };





}