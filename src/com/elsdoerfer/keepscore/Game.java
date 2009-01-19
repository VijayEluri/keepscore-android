package com.elsdoerfer.keepscore;

import java.lang.reflect.Array;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Game extends Activity {
	
	protected static CharSequence[] mPlayers = 
		{"Michael", "Peter", "Paul", "Sebastian"}; 
	
	// resources
	protected Typeface mBoldFace;
	protected int mCellPadding;
	
	// static views
	protected ScrollView mGameScrollView;
	protected TableLayout mGameTable;
	protected Button mAddNewScoresButton;
	
	// dynamic views
	protected TableRow mHeaderRow;
	protected TableRow mFooterRow;
	protected EditText[] mNewScoreEdits;
	
	// holds the user-entered or automatically calculated 
	// values for the new scores.
	protected Integer[] mNewScoreValues;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        
        // get views
        mGameScrollView = (ScrollView)findViewById(R.id.game_container);
        mGameTable = (TableLayout)findViewById(R.id.game);
        mAddNewScoresButton = (Button)findViewById(R.id.add_new_scores);
        TableRow.LayoutParams params = (TableRow.LayoutParams)mAddNewScoresButton.getLayoutParams();
        params.span = mPlayers.length;
        mAddNewScoresButton.setLayoutParams(params);
                
        // load resources
        mBoldFace = Typeface.defaultFromStyle(Typeface.BOLD);
        mCellPadding = getResources().getDimensionPixelSize(R.dimen.game_table_padding);
        
        // create header row, listing the names of the players
        mHeaderRow = makeTextRow(mPlayers, true);
        mGameTable.addView(mHeaderRow, 0);
        
        // create the edit row, allows adding new scores
        TableRow editRow = new TableRow(this);        
        editRow.setPadding(mCellPadding, mCellPadding, mCellPadding, mCellPadding);
        mNewScoreEdits = (EditText[]) Array.newInstance(EditText.class, mPlayers.length);
        mNewScoreValues = (Integer[]) Array.newInstance(Integer.class, mPlayers.length);
        for (int i=0; i<mPlayers.length; i++) {
        	EditText edit = new EditText(this);
        	edit.setGravity(Gravity.CENTER);
        	// We'd want single-line, most importantly since <enter> would then
        	// jump to the submit button automatically, but alas, there seems to
        	// be a bug in Android 1.0 which causes the hint-text not to show 
        	// if single line is enabled. So for now, don't.
        	// edit.setSingleLine(true);
        	
        	// Columns growing and shrinking while the user is entering text
        	// can be slow when the game table has lots of rows.
        	// TODO: Find the best/right value 
        	edit.setMaxWidth(30);  
        	
        	// Use a DigitsKeyListener to only allow digits, plus add some 
        	// custom key handling. 
        	edit.setKeyListener(new DigitsKeyListener(true, false) {

				@Override
				public boolean onKeyDown(View view, Editable text, int keyCode,
						KeyEvent event) {
					return super.onKeyDown(view, text, keyCode, event);
				}

				@Override
				public boolean onKeyUp(View view, Editable text, int keyCode,
						KeyEvent event) {
					// If the user presses enter, jump to the submit button 
					// below. This is basically copied from 
					// android.widget.TextView.java. It would be normal 
					// behavior if single-line where true, but we cannot 
					// use that due to a bug in Android (hints would not show).
					switch (keyCode) {
					case KeyEvent.KEYCODE_ENTER:
						View v = view.focusSearch(View.FOCUS_DOWN);
						if (v!=null) v.requestFocus(View.FOCUS_DOWN);
						return true;
					}
										
					boolean result = super.onKeyUp(view, text, keyCode, event);
					// update user interface to this change
					updateUI();
					return result;
				}
        		
        	});
        	edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {					
					// Some error messages need to update when the
					// focus changes, see updateUI comments for more info.
					updateUI();					
				}        		
        	});
        	editRow.addView(edit);
        	mNewScoreEdits[i] = edit;
        	mNewScoreValues[i] = null;
        }
        mGameTable.addView(editRow, 1);                     
        
        // setup event handlers     
        mAddNewScoresButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Add new row of scores.
				// We trust that mNewScoreValues contains no null values.
				// The submit button should not be enabled if this is not
				// the case.
		        TableRow newRow = makeTextRow(mNewScoreValues, false);
		        mGameTable.addView(newRow, mGameTable.getChildCount()-2);
		        // clear existing input values.
		        for (int i=0; i<mNewScoreValues.length; i++)	{
		        	mNewScoreValues[i] = null;
		        	mNewScoreEdits[i].setText("");
		        }
		        // update UI		        
		        //mNewScoreEdits[0].requestFocus();
		        // TODO: for some reason, this still doesn't scroll to the very bottom
		        mGameScrollView.fullScroll(View.FOCUS_DOWN);
		        mGameScrollView.scrollBy(0, mAddNewScoresButton.getHeight());
		        updateUI();
			}
        });
        
        updateUI();
	}
	
	/**
	 * Make a new row of TextView objects that can be added to the
	 * table.
	 * 
	 * Used to create the score rows for each round, as well as 
	 * header rows. 
	 */
	protected TableRow makeTextRow(Object[] values, boolean header) {
		TableRow newRow = new TableRow(this);
        for (Object value : values) {
        	TextView text = new TextView(this);
        	text.setText(value.toString());
        	text.setGravity(Gravity.CENTER);
        	if ( header) text.setTypeface(mBoldFace);
        	text.setPadding(mCellPadding, mCellPadding, mCellPadding, mCellPadding);
        	newRow.addView(text);        	
        }
        return newRow;
	}
	
	protected void updateUI() {
		// Calculate automatic values for the scores the user did not input himself.
		int numManualScores = 0;  
		int sumManualScores = 0;  
		for (int i=0; i<mNewScoreEdits.length; i++) {
			EditText scoreEdit = mNewScoreEdits[i];
			String stringValue = scoreEdit.getText().toString();
        	if (stringValue.length() != 0)
	        	try {
	        		int intValue = Integer.parseInt(stringValue);
	        		mNewScoreValues[i] = intValue;
	        		sumManualScores += intValue;
	        		numManualScores++;
	        	} catch (NumberFormatException e) {
	        		// Android's error message functionality is nice,
					// but because it is reset every time the user 
					// changes the text, and this could would immediately
					// set the error again, you'd see the error popup
					// constantly hiding/showing as the user types, which
					// doesn't look good and is slow. So our hacky solution
					// is to only show set the error if the edit does not
					// have focus, i.e. once it loses focus.
					if (!scoreEdit.hasFocus())						
						scoreEdit.setError(getResources().getString(R.string.not_a_valid_number)); 
	        	}
        	else {
        		// This particular field has no explicit value, indicate 
        		// as much so that it will later be calculated. 
        		mNewScoreValues[i] = null;
        	}
		}
		// Provide default values for the fields currently empty.
		int mNumAutomaticValues = mNewScoreValues.length-numManualScores;
		for (int i=0; i<mNewScoreEdits.length; i++) {
			EditText scoreEdit = mNewScoreEdits[i];			
			if (!scoreEdit.isEnabled())
				scoreEdit.setEnabled(true);
			
			// Nothing to suggest if not a single value provided by user;
			// simple clear all automatic values.
			if (numManualScores<=0) {
				scoreEdit.setHint(null);
				mNewScoreValues[i] = null;
			}			
			else {
				// If this is an empty field, provide it with an automatic value 
				if (scoreEdit.getText().length() == 0) {
					int suggestedValue = -(sumManualScores / mNumAutomaticValues);
					scoreEdit.setHint(String.valueOf(suggestedValue));  // TODO: support floats								
					mNewScoreValues[i] = suggestedValue;
					// If there is only one automatic field left, disable it.
					// This prevents the user from entering unbalanced values.
					if (mNumAutomaticValues==1)
						scoreEdit.setEnabled(false);
				}
			}
		}	

		// enable submit button if the user provided at least one score
		mAddNewScoresButton.setEnabled(numManualScores>0);					
	}
	
	// TODO: menu options to add: remove last row, leave game
	
	// TODO: automatically use last value when entering a new field
	
    /*
	 * TODO: add horizontal scrolling:
	 * 	http://nan-technicalblog.blogspot.com/2008/12/android-how-to-use-horizontal-scrolling.html
	 *  http://groups.google.com/group/android-developers/browse_thread/thread/bf080f10774979b8/28df4ce43e6b2916?show_docid=28df4ce43e6b2916
	 *  http://www.anddev.org/scrolling_a_picture_horizontally_and_vertically-t3245.html
	 */

}
