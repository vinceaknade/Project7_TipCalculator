package com.beech.tipcalculator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

public class TipCalculatorActivity extends Activity 
implements OnEditorActionListener, OnClickListener {

    // define variables for the widgets
    private EditText billAmountEditText;
    private TextView percentTextView;   
    private Button   percentUpButton;
    private Button   percentDownButton;
    private Button   btnSave;
    private TextView tipTextView;
    private TextView totalTextView;
    
    // define instance variables that should be saved
    private String billAmountString = "";
    private float tipPercent = .15f;
    private float billAmount = 0f;

    private TipDatabase db;
    
    // set up preferences
    private SharedPreferences prefs;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_calculator);

        // get db and StringBuilder objects
        db = new TipDatabase(this);
        
        // get references to the widgets
        billAmountEditText = (EditText) findViewById(R.id.billAmountEditText);
        percentTextView = (TextView) findViewById(R.id.percentTextView);
        percentUpButton = (Button) findViewById(R.id.percentUpButton);
        percentDownButton = (Button) findViewById(R.id.percentDownButton);
        btnSave = (Button) findViewById(R.id.btnSave);
        tipTextView = (TextView) findViewById(R.id.tipTextView);
        totalTextView = (TextView) findViewById(R.id.totalTextView);

        // set the listeners
        billAmountEditText.setOnEditorActionListener(this);
        percentUpButton.setOnClickListener(this);
        percentDownButton.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        
        // get default SharedPreferences object
        prefs = PreferenceManager.getDefaultSharedPreferences(this);        
    }
    
    @Override
    public void onPause() {
        // save the instance variables       
        Editor editor = prefs.edit();        
        editor.putString("billAmountString", billAmountString);
        editor.putFloat("tipPercent", tipPercent);
        editor.commit();        

        super.onPause();      
    }
    
    @Override
    public void onResume() {
        super.onResume();

        //get database values and log
        ArrayList<Tip> tips = db.getTips();//<== already had the collection here shouldn't have to average data in the database access layer
        Tip finalTip = new Tip();

        for(Tip tip: tips)
        {
            Log.i("Tip", "\n"
                    + "Bill date: " + tip.getDateStringFormatted() + "\n"
                    + "Bill amount: " + tip.getBillAmountFormatted() + "\n"
                    + "Tip percent: " + tip.getTipPercentFormatted() + "\n"
                    );
            finalTip = tip;
        }

        //Toasts do not work well on resume, or maybe not at all on resume
        //Toast.makeText(getApplicationContext(),finalTip.getDateStringFormatted(),Toast.LENGTH_LONG);
        Log.i("Last tip saved:", finalTip.getDateStringFormatted());

        float averageTip = db.getAverageTip();
        Log.i("Average Tip is:", averageTip + "");

        // get the instance variables
        billAmountString = prefs.getString("billAmountString", "");
        tipPercent = prefs.getFloat("tipPercent", 0.15f);

        // set the bill amount on its widget
        billAmountEditText.setText(billAmountString);
        
        // calculate and display
        calculateAndDisplay();
    }
    
    public void calculateAndDisplay() {        

        // get the bill amount
        billAmountString = billAmountEditText.getText().toString();
        if (billAmountString.equals("")) {
            billAmount = 0;
        }
        else {
            billAmount = Float.parseFloat(billAmountString);
        }
        
        // calculate tip and total 
        float tipAmount = billAmount * tipPercent;
        float totalAmount = billAmount + tipAmount;
        
        // display the other results with formatting
        NumberFormat currency = NumberFormat.getCurrencyInstance();
        tipTextView.setText(currency.format(tipAmount));
        totalTextView.setText(currency.format(totalAmount));
        
        NumberFormat percent = NumberFormat.getPercentInstance();
        percentTextView.setText(percent.format(tipPercent));
    }
    
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE ||
    		actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            calculateAndDisplay();
        }        
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.percentDownButton:
            tipPercent = tipPercent - .01f;
            calculateAndDisplay();
            break;
        case R.id.percentUpButton:
            tipPercent = tipPercent + .01f;
            calculateAndDisplay();
            break;
            case R.id.btnSave:
            if(validated())
            {
                //insert tip to database
                Tip tip = new Tip();
                tip.setTipPercent(tipPercent);
                tip.setBillAmount(billAmount);
                db.insertTip(tip);

                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT);

                //clear billamount field
                billAmountEditText.setText("");

                float averageTip = db.getAverageTip();
                percentTextView.setText(averageTip + "");
            }
        }
    }

    //validate contents of controls
    public boolean validated()
    {
        boolean valid = true;

        if(billAmount == 0f)
            valid = false;

        return valid;
    }
}