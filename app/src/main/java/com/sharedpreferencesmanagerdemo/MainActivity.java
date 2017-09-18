package com.sharedpreferencesmanagerdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.sharedpreferencesmanager.generated.SPManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String[] SP_VALUES = {"test1", "test2", "test3", "test4"};

    private Spinner mSPName;
    private EditText mSPValue;
    private Button mSubmit;

    private SPManager mMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSPName = findViewById(R.id.spmgrTest_name);
        mSPValue = findViewById(R.id.spmgrTest_value);
        mSubmit = findViewById(R.id.spmgrTest_submit);

        mSubmit.setOnClickListener(this);

        mMgr = SPManager.getInstance(getSharedPreferences(getPackageName(), MODE_APPEND));
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mSubmit)) {
            int spinnerValue = mSPName.getSelectedItemPosition();
            mMgr.update(this, SP_VALUES[spinnerValue], mSPValue.getText().toString());
        }
    }
}
