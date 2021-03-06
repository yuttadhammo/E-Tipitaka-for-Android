package org.yuttadhammo.tipitaka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.Typeface;

public class ReadBookActivity extends SherlockFragmentActivity {

	// page flipping
	
    public static final String TAG = "ReadBookActivity";
	private static ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    
	private static SharedPreferences prefs;
	private static MainTipitakaDBAdapter mainTipitakaDBAdapter;

	private static String headerText = "";
	private ListView idxList;
	
	private static Button dictButton;
	private static LinearLayout dictBar;
	private static TextView defText;
	
	private static int selected_volume;
	private int selected_page;
	
	private View read;
	private static String keywords = "";
	private Dialog itemsDialog;
	private Dialog memoDialog;
	private static String savedItems;
	private static String lang = "pali";
	private float textSize = 0f;
	private String bmLang;
	private int bmVolume;
	private int bmPage;
	private int bmItem;
	private EditText memoText;
	private BookmarkDBAdapter bookmarkDBAdapter = null;
	private SearchDialog searchDialog = null;
	
	public int oldpage = 0;
	public int newpage = 0;

    private static String[] volumes;
	private Typeface font;

	// save read pages for highlighting in the results list

	private static ArrayList<String> savedReadPages = null;


	public static Resources res;
	protected int lastPosition;
	private ArrayList<String> titles;
	private static String volumeTitle;
	private float smallSize;
	private static String scrollString;
	protected boolean lastPage;
	private int NUM_PAGES = 0;
	private SlidingMenu slideMenu;
	private ActionBar actionBar;
	public static ScrollView scrollview;
	protected static PaliTextView textContent;
	public static Spanned spannedText;
	private static Context context;
	public static boolean isLookingUp = false;
	private static boolean lookupDefs;
	private static int searchColor;
	private int findColor;
	private static int titleColor;
	private static int varColor;
	private static int textColor;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        context = getApplicationContext();
        prefs =  PreferenceManager.getDefaultSharedPreferences(context);
		font = Typeface.createFromAsset(getAssets(), "verajjan.ttf");      
        
        lookupDefs = prefs.getBoolean("show_defs", true);
        
        read =  View.inflate(this, R.layout.read, null);
        setContentView(read);

        mainTipitakaDBAdapter = new MainTipitakaDBAdapter(this);
        
        savedReadPages = new ArrayList<String>();

        bookmarkDBAdapter = new BookmarkDBAdapter(this);

        res = getResources();

		dictBar = (LinearLayout) findViewById(R.id.dict_bar);
		defText = (TextView) findViewById(R.id.def_text);
		dictButton = (Button) findViewById(R.id.dict_button);

        slideMenu = new SlidingMenu(this);
        slideMenu.setMode(SlidingMenu.LEFT);
        slideMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        //menu.setShadowWidthRes(0);
        //menu.setShadowDrawable(R.drawable.shadow);
        slideMenu.setBehindWidthRes(R.dimen.slide_width);
        slideMenu.setFadeDegree(0.35f);
        slideMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        slideMenu.setMenu(R.layout.slide);
		slideMenu.setSlidingEnabled(false);
		
		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
        mPager = (ViewPager) findViewById(R.id.pager);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}

			@Override
			public void onPageSelected(int arg0) {
				int modifier = (lastPosition == arg0-1?1:(lastPosition == arg0+1?-1:0));
				lastPosition = arg0;
				
				updateIndexList(modifier);
			}
        	
        });
        
        defText.setTypeface(font);
        
		
		dictButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				View fc = mPager.getFocusedChild();
				if(fc == null)
					return;
				textContent = (PaliTextView) fc.findViewById(R.id.main_text);

				if(!textContent.hasSelection())
					return;
				int s=textContent.getSelectionStart();
				int e=textContent.getSelectionEnd();
				String word = textContent.getText().toString().substring(s,e);
				word = word.replaceAll("/ .*/","");
				
				final String aword = word;
				
				Intent intent = new Intent(getBaseContext(), DictionaryActivity.class);
				Bundle dataBundle = new Bundle();
				dataBundle.putString("word", aword);
				dataBundle.putInt("dict", 4);
				intent.putExtras(dataBundle);
				startActivity(intent);

			}
		});

        // index button

        idxList = (ListView) slideMenu.findViewById(R.id.index_list);
        
        try {
        	mainTipitakaDBAdapter.open();
        	if(mainTipitakaDBAdapter.isOpened()) {
        		mainTipitakaDBAdapter.close();
        	} else {
            	Downloader dl = new Downloader(this);
            	dl.startDownloader("http://static.sirimangalo.org/pali/ATPK/ATPK.zip", "ATPK.zip");
        		return;
        	}
        } catch (SQLiteException e) {
			Log.e ("Tipitaka","error:", e);
        	Downloader dl = new Downloader(this);
        	dl.startDownloader("http://static.sirimangalo.org/pali/ATPK/ATPK.zip", "ATPK.zip");
        	return;
        }
        
		// hide virtual keyboard
//		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.hideSoftInputFromWindow(textContent.getWindowToken(), 0);

		read.requestLayout();
        
		titles = new ArrayList<String>();
              
		if(getIntent().getExtras() != null) {
			Bundle dataBundle = getIntent().getExtras();
			selected_volume = dataBundle.getInt("VOL");
			
			volumes = res.getStringArray(R.array.volume_names);
			volumeTitle = volumes[selected_volume].trim();

			lastPosition = dataBundle.getInt("PAGE");
			Log.i("Initial Page Number: ",lastPosition+"");
			
			if (!dataBundle.containsKey("FIRSTPAGE")) {
			}
			
			lang = dataBundle.getString("LANG");
			
			savedReadPages.clear();
			
			if (dataBundle.containsKey("QUERY")) {
				keywords = dataBundle.getString("QUERY");
				scrollString = keywords.split("\\+")[0];
			} 
			
			// create index
			
			mainTipitakaDBAdapter.open();
			Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume);

			NUM_PAGES = cursor.getCount();
			
			cursor.moveToFirst();

			while (!cursor.isAfterLast()) {
				String title = cursor.getString(1);
				titles.add(formatTitle(title));
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			mainTipitakaDBAdapter.close();

			idxList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					lastPosition = position;
					mPager.setCurrentItem(lastPosition);
					slideMenu.toggle();
			  	}
			});

	        mPagerAdapter = new ScreenSlidePagerAdapter(this.getSupportFragmentManager());
	        mPager.setAdapter(mPagerAdapter);
			IndexItemAdapter adapter = new IndexItemAdapter(ReadBookActivity.this, R.layout.index_list_item, R.id.title, titles, lastPosition);
			idxList.setAdapter(adapter);
	        updatePage(0);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		saveReadingState("thai", 1, 0);
		saveReadingState("pali", 1, 0);
		
		//textContent.setOnTouchListener(otl);
		//scrollview.setOnTouchListener(otl);
		//if (dbhelper != null && dbhelper.isOpened())
		//	dbhelper.close();
		//if(bookmarkDBAdapter != null)
		//	bookmarkDBAdapter.close();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		String size = prefs.getString("base_text_size", "16");
		if(size.equals(""))
			size = "16";
		textSize = Float.parseFloat(size);
		smallSize = Float.parseFloat(Double.toString(textSize*0.75));
		defText.setTextSize(smallSize);

		if(searchDialog != null) {
			searchDialog.updateHistoryList();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		read.setBackgroundColor(prefs.getInt("text_color_back", getResources().getColor(R.color.text_color_back)));

		String size = prefs.getString("base_text_size", "16");
        if(size.equals(""))
        	size = "16";
		textSize = Float.parseFloat(size);
        smallSize = Float.parseFloat(Double.toString(textSize*0.75));
		defText.setTextSize(smallSize);

		textColor = prefs.getInt("text_color", getResources().getColor(R.color.text_color));
		titleColor = prefs.getInt("text_color_title", getResources().getColor(R.color.text_color_title));
		varColor = prefs.getInt("text_color_var", getResources().getColor(R.color.text_color_var));
		searchColor = prefs.getInt("text_color_search", getResources().getColor(R.color.text_color_search));
		findColor = prefs.getInt("text_color_find", getResources().getColor(R.color.text_color_find));

		View fc = mPager.getChildAt(lastPosition);
    	
		if(fc != null) {
			textContent = (PaliTextView) fc.findViewById(R.id.main_text);
			textContent.setTextColor(prefs.getInt("text_color", getResources().getColor(R.color.text_color)));
		}
	}

    @Override
    public boolean onSearchRequested() {
		Intent intent = new Intent(this, SearchDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
    	return true;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = this.getSupportMenuInflater();
	    inflater.inflate(R.menu.read_menu, menu);
	    return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		super.onOptionsItemSelected(item);
		Bundle dataBundle = new Bundle();
		
		//SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            finish();
	            return true;
			case R.id.index:
				slideMenu.toggle();
				break;	            
			case R.id.find:
				searchInPage();
				break;	            
			case (int)R.id.help_menu_item:
				intent = new Intent(this, HelpActivity.class);
				startActivity(intent);
				break;
			case (int)R.id.read_bookmark:
				intent = new Intent(ReadBookActivity.this, BookmarkPaliActivity.class);
				dataBundle.putString("LANG", lang);
				intent.putExtras(dataBundle);
				startActivity(intent);	
				break;
			case (int)R.id.memo:
				prepareBookmark();
				break;
			case (int)R.id.prefs_read:
				intent = new Intent(this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.read_dict_menu_item:
				intent = new Intent(this, DictionaryActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.english_menu_item:
				intent = new Intent(this, EnglishActivity.class);
				String url = getTrans();
				if(url != null) {
					dataBundle.putString("url", url);
					intent.putExtras(dataBundle);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			default:
				return false;
	    }
		return true;
	}	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
			case KeyEvent.KEYCODE_MENU:
				break;
			case KeyEvent.KEYCODE_SEARCH:
				break;
			case KeyEvent.KEYCODE_BACK:
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if(prefs.getBoolean("vol_nav", false)) {
					if(prefs.getBoolean("vol_nav_reverse", false))
						readPrev();
					else
						readNext();
					return true;
				}
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if(prefs.getBoolean("vol_nav", false)) {
					if(prefs.getBoolean("vol_nav_reverse", false))
						readNext();
					else
						readPrev();
					return true;
				}
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
			case KeyEvent.KEYCODE_MENU:
				break;
			case KeyEvent.KEYCODE_SEARCH:
				break;
			case KeyEvent.KEYCODE_BACK:
				Intent result = new Intent();
				//Toast.makeText(this, savedReadPages.toString(), Toast.LENGTH_SHORT).show();
				
				String [] tmp = new String[savedReadPages.size()];
				savedReadPages.toArray(tmp);

				result.putExtra("READ_PAGES", tmp);
				setResult(RESULT_CANCELED, result);
					
				this.finish();
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
		        return true;
		}

	    return super.onKeyUp(keyCode, event);
	}
	
	private void saveBookmark(int volume, int item, int page, String language) {
		memoDialog = new Dialog(ReadBookActivity.this);
		memoDialog.setContentView(R.layout.memo_dialog);
		
		bmLang = language;
		bmVolume = volume;
		bmPage = page;
		bmItem = item;
		
		
		Button memoBtn = (Button)memoDialog.findViewById(R.id.memo_btn);
		memoText = (EditText)memoDialog.findViewById(R.id.memo_text);
		memoText.setTypeface(font);
		memoText.setText(headerText);
		
		memoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bookmarkDBAdapter.open();
				BookmarkItem bookmarkItem = new BookmarkItem(bmLang, bmVolume, bmPage, bmItem, memoText.getText().toString(),"");
				bookmarkDBAdapter.insertEntry(bookmarkItem);
				bookmarkDBAdapter.close();
				Toast.makeText(ReadBookActivity.this, getString(R.string.memo_set), Toast.LENGTH_SHORT).show();
				memoDialog.dismiss();
			}
		});
		
		memoDialog.setCancelable(true);
		//~ String title1 = "";
		//~ if(lang.equals("thai")) {
			//~ title1 = getString(R.string.th_tipitaka_label) + " " + getString(R.string.th_lang);
		//~ } else if(language.equals("pali")) {
			//~ title1 = getString(R.string.th_tipitaka_label) + " " + getString(R.string.pl_lang);
		//~ }
		//~ TextView sub_title = (TextView)memoDialog.findViewById(R.id.memo_sub_title);
		//~ String title2 = getString(R.string.th_book_label) + " " + Utils.arabic2thai(Integer.toString(volume), getResources());
		//~ title2 = title2 + " " + getString(R.string.th_page_label) + " " + Utils.arabic2thai(Integer.toString(page), getResources());
		//~ title2 = title2 + " " + getString(R.string.th_items_label) + " " + Utils.arabic2thai(Integer.toString(item), getResources());
		//~ sub_title.setText(title2);
		memoDialog.setTitle(getString(R.string.memoTitle));
		memoDialog.show();
	}
	
	private void prepareBookmark() {
		String [] items = savedItems.split("\\s+");
		selected_page = lastPosition-1;

		if(items.length > 1) {
			itemsDialog = new Dialog(ReadBookActivity.this);						
			itemsDialog.setContentView(R.layout.select_dialog);
			itemsDialog.setCancelable(true);
			
			itemsDialog.setTitle(getString(R.string.select_item_memo));
			
			ListView pageView = (ListView) itemsDialog.findViewById(R.id.list_pages);						
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ReadBookActivity.this, R.layout.page_item, R.id.show_page);			

			for(String item : items) {
				dataAdapter.add(getString(R.string.th_items_label) + " " + Utils.arabic2thai(item, getResources()));
			}
			
			pageView.setAdapter(dataAdapter);			
			pageView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					String [] items = savedItems.split("\\s+");
					saveBookmark(selected_volume, Integer.parseInt(items[arg2]), selected_page, lang);
					itemsDialog.dismiss();
				}
				
			});			
			itemsDialog.show();			
		} else {
			saveBookmark(selected_volume, Integer.parseInt(items[0]), selected_page, lang);
		}		
	}
			
	private void saveReadingState(String _lang, int page, int scrollPosition) {
		SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(_lang+":PAGE", page);
    	editor.putInt(_lang+":POSITION", scrollPosition);
    	editor.commit();		
	}
	
	private void readNext() {
		if(lastPosition+1 < idxList.getCount()) {
			mPager.setCurrentItem(lastPosition+1);
		}		
	}
	
	private void readPrev() {
		if(lastPosition > 0) {
			mPager.setCurrentItem(lastPosition-1);
		}		
	}

	private void updatePage(int modifier) {

		// hide index
	
		mPager.setCurrentItem(lastPosition);
        
		updateIndexList(modifier);

	}

	private void updateIndexList(int modifier) {
		
		Log.i(TAG,"update index list modifier: "+modifier);
		
        int index = idxList.getFirstVisiblePosition();
        View v = idxList.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        int firstPosition = index - idxList.getHeaderViewsCount(); // This is the same as child #0
        int wantedChild = lastPosition - firstPosition;
        int bottom = 0;
        
        if(modifier != 0) {
	        // Say, first visible position is 8, you want position 10, wantedChild will now be 2
	        // So that means your view is child #2 in the ViewGroup:
	        if (wantedChild < 0 || wantedChild >= idxList.getChildCount()) {
	          Log.w(TAG, "Unable to get view for desired position, because it's not being displayed on screen.");
	        }
	        else {
		        // Could also check if wantedPosition is between listView.getFirstVisiblePosition() and listView.getLastVisiblePosition() instead.
		        View vl = idxList.getChildAt(wantedChild-1);
				if(vl != null)
					bottom = vl.getHeight();
	        }
        }
        
		IndexItemAdapter adapter = new IndexItemAdapter(ReadBookActivity.this, R.layout.index_list_item, R.id.title, titles, lastPosition);
		idxList.setAdapter(adapter);
		
		top -= (bottom*modifier);
		
		Log.i(TAG,"top modifier: "+top);
		
		// restore
        idxList.setSelectionFromTop(index, top);		
	}


	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
		
        public ScreenSlidePagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
		}


		@Override
        public Fragment getItem(int position) {
            ScreenSlidePageFragment sspf = new ScreenSlidePageFragment();
            Bundle args = new Bundle();
            args.putInt("position", position);
            sspf.setArguments(args);
            return sspf;
        }
		@Override
        public int getCount() {
			return NUM_PAGES;
        }

    }
	
	private static void pageText(int position) {
		//Log.i ("Tipitaka","get volume: "+selected_volume);
		savedReadPages.add(selected_volume+":"+(position+1));
		mainTipitakaDBAdapter.open();
		Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume, position, lang);
		
		if(cursor == null || cursor.getCount() == 0) {
			cursor.close();
			mainTipitakaDBAdapter.close();
			return;
		}
		
		cursor.moveToFirst();
		//Log.i ("Tipitaka","db cursor length: "+cursor.getCount());
		String title = cursor.getString(2);
		String content = cursor.getString(1);
		
		if(content == null)
			content = "";
		content = content.replaceAll("\\[[0-9]+\\]", "");

		// highlight items numbers (orange)
		//content = content.replaceAll(getString(R.string.regex_item), "<font color='#EE9A00'><b>$0</b></font>");
		
		content = content.replaceAll("\\^b\\^", "<b>");
		content = content.replaceAll("\\^eb\\^", "</b>");
		content = content.replaceAll("\\^a\\^[^^]+\\^ea\\^", "");
		content = content.replaceAll("'''", "’”");
		content = content.replaceAll("''", "”");
		content = content.replaceAll("'", "’");
		content = content.replaceAll("``", "“");
		content = content.replaceAll("`", "‘");
		content = content.replaceAll("([AIUEOKGCJTDNPBMYRLVSHaiueokgcjtdnpbmyrlvshāīūṭḍṅṇṁṃñḷĀĪŪṬḌṄṆṀṂÑḶ])0", "$1.");
	
		// highlight keywords (yellow)
		if(keywords.trim().length() > 0) {
			Log.i("Tipitaka","keywords: "+ keywords);
			keywords = keywords.replace('+', ' ');
			String [] tokens = keywords.split("\\+");
			Arrays.sort(tokens, new StringLengthComparator());
			Collections.reverse(Arrays.asList(tokens));
			for(String token: tokens) {
				content = content.replace(token, "<font color='"
						+String.format("#%06X", 
								(0xFFFFFF & searchColor)
							)
						+"'>"
						+token
						+"</font>"
					);
			}
		}
		
		if(prefs.getBoolean("show_var", true))
			content = content.replaceAll("\\{([^}]+)\\}", "<font color='"
				+String.format("#%06X", 
						(0xFFFFFF & varColor)
					)
				+"'>[$1]</font>");
		else
			content = content.replaceAll("\\{([^}]+)\\}", "");
		
		title = formatTitle(title);
		headerText = volumeTitle+", " + title;
		content = "<font color='"
				+String.format("#%06X", 
						(0xFFFFFF & titleColor)
					)
				+"'>"+headerText+"</font><br/><br/>"+content.replace("\n", "<br/>");
		Spanned html = Html.fromHtml(content);
						
		savedItems = cursor.getString(0);	
		cursor.close();
		mainTipitakaDBAdapter.close();
		
		volumes = res.getStringArray(R.array.volume_names);

		spannedText = html;
	}

	public String getTrans() {
		String vols = Integer.toString(selected_volume-1);
		String[] list = res.getStringArray(R.array.sut_m_list);
		if(!Arrays.asList(list).contains(vols))
			return null;
		int i;
		for(i = 0; i < list.length; i++) {
			if(list[i].equals(vols)) {
				break;
			}
		}
		String[] names = res.getStringArray(R.array.sut_m_names);
		String name = names[i];

		char nik = name.charAt(0);

		return "file://"+prefs.getString("ati_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/ati_website")+"/html/tipitaka/"+nik+"n/index.html";
		
	}

	public static String formatTitle(String title) {
		title = title.replaceAll("\\^+", "^");
		title = title.replaceAll("^\\^", "");
		title = title.replaceAll("\\^$", "");
		title = title.replaceAll("\\^", ", ");
		return title;
	}
	

	public static class PaliTextView extends TextView {

		public PaliTextView(Context context, AttributeSet attrs, int defStyle)
		{
		    super(context, attrs, defStyle);
		}   
		
		
		public PaliTextView(Context context, AttributeSet attrs)
		{
		    super(context, attrs);
		}
		
		public PaliTextView(Context context)
		{
		    super(context);
		}
		
	    @Override   
	    protected void onSelectionChanged(int s, int e) {
	    	if(mPager.getChildCount() == 0)
	    		return;
			defText.setVisibility(View.INVISIBLE);
	    	if(s > -1 && e > s) {
				
				String selectedWord = this.getText().toString().substring(s,e);
	    		Log.i("Selected word",selectedWord);
				if(selectedWord.contains(" "))
	    			dictBar.setVisibility(View.INVISIBLE);
	    		else {
    	    		dictBar.setVisibility(View.VISIBLE);
					if(lookupDefs && !isLookingUp) {
						LookupDefinition ld = new LookupDefinition();
						ld.execute(selectedWord);
					}
	    		}
	    	}
		    else
		    	dictBar.setVisibility(View.INVISIBLE);
	    }
	    private class LookupDefinition extends AsyncTask<String, Integer, String> {
	    	private MainTipitakaDBAdapter db;
			private String definition;

	    	@Override
	        protected String doInBackground(String... words) {
	        	String query = PaliUtils.toVel(words[0]);
	        	//Log.d ("Tipitaka", "looking up: "+query);
				
				db = new MainTipitakaDBAdapter(context);
	    		db.open();

				String[] declensions = getResources().getStringArray(R.array.declensions);
				ArrayList<String> endings = new ArrayList<String>(); 
				for(String declension : declensions) {
					String[] decArray = TextUtils.split(declension, ",");
					String ending = decArray[0];
					int offset = Integer.parseInt(decArray[1]);
					int min = Integer.parseInt(decArray[2]);
					String add = decArray[3];
					if(query.length() > min && query.endsWith(ending)) {
						endings.add(TextUtils.substring(query, 0, query.length()-ending.length()+offset)+add);
						//Log.d ("Tipitaka", "adding ending: " + endings.get(endings.size()-1));
					}
				}
				if(endings.isEmpty())
					return null;

				String endstring = "'"+TextUtils.join("','", endings)+"'";

				Cursor c = db.dictQueryEndings("cped",endstring);	
	    		
		    	if(c == null || c.getCount() == 0) {
					c = db.dictQuery("cped",query);

			    	if(c == null || c.getCount() == 0)
						return null;
		    	}
		    	c.moveToFirst();
		    	definition = "<b>"+PaliUtils.toUni(c.getString(0))+":</b> "+c.getString(1);
				return null;
	        }
	        @Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            isLookingUp = false;
	        }

	        @Override
	        protected void onProgressUpdate(Integer... progress) {
	            super.onProgressUpdate(progress);
	        }

	        @Override
	        protected void onPostExecute(String result) {
	            super.onPostExecute(result);
				db.close();
	            if(definition != null) {
	            	defText.setText(Html.fromHtml(definition));
		    		defText.setVisibility(View.VISIBLE);
	            }
	            isLookingUp = false;
	            
			}

	    }
	    @Override
	    protected void onLayout (boolean changed, int left, int top, int right, int bottom){
	    	if(scrollString != null) {
		    	 new CountDownTimer(500, 500) {

		    	     public void onTick(long millisUntilFinished) {
		    	     }

		    	     public void onFinish() {
		 				View fc = mPager.getFocusedChild();
						if(fc == null)
							return;
						
						textContent = (PaliTextView) fc.findViewById(R.id.main_text);
						scrollview = (ScrollView) fc.findViewById(R.id.scroll_text);
						if(textContent.getLayout() == null)
							return;
						Log.i("Tipitaka","Keyword to scroll: "+scrollString);
						int offset =  textContent.getText().toString().indexOf(scrollString);
						int jumpLine = textContent.getLayout().getLineForOffset(offset);
						int y=0;
						if(jumpLine > 2)
							y = textContent.getLayout().getLineTop(jumpLine-2);
						else
							y = textContent.getLayout().getLineTop(0);
						scrollview.scrollTo(0, y);
		    	     }
		    	  }.start();
			}
	    }
	}
	
	class IndexListView extends ListView {

		public IndexListView(Context context) {
			super(context);
		}
		
		@Override
	    protected void onLayout (boolean changed, int left, int top, int right, int bottom){
			idxList.setSelectionFromTop(idxList.getChildAt(lastPosition).getTop(),lastPosition);

		}
	}

	public static class ScreenSlidePageFragment extends Fragment {
		private ViewGroup rootView;
	    public PaliTextView textView;

		@SuppressLint("NewApi")
		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
	        rootView = (ViewGroup) inflater.inflate(
	                R.layout.page, container, false);
            int position = this.getArguments().getInt("position");
			pageText(position);
	        textView = (PaliTextView) rootView.findViewById(R.id.main_text);
	        textView.setText(spannedText);
			Typeface font = Typeface.createFromAsset(this.getActivity().getAssets(), "verajjan.ttf");      
	        textView.setTypeface(font);
	        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this.getActivity());
			String size = prefs.getString("base_text_size", "16");
			if(size.equals(""))
				size = "16";
			Float textSize = Float.parseFloat(size);
			textView.setTextSize(textSize);
			textView.setTextColor(textColor);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			@SuppressWarnings("deprecation")
			int api = Integer.parseInt(Build.VERSION.SDK);
			
			if (api >= 14) {
				textView.setTextIsSelectable(true);
			}
			
	        return rootView;
	    }
	}
	
	
	int atSearch = 0;
	private String currentSearchText;
	private EditText findBox;
	private CharSequence virginText;
	
    public void searchInPage(){
    	
    	View fc = mPager.getChildAt(lastPosition);
		textContent = (PaliTextView) fc.findViewById(R.id.main_text);
		virginText = textContent.getText();
    	
    	findBox = new EditText(this);
    	atSearch = 0;
    	
	    final LinearLayout container = (LinearLayout)findViewById(R.id.search);
	    
	    if(container.getChildCount() > 0)
	    	return;

	    Button nextButton = new Button(this);  
	    nextButton.setText("Next");
	    nextButton.setOnClickListener(new OnClickListener(){  
	    	@Override  
	    	public void onClick(View v){
	    		
	    		if(!findBox.getText().toString().equals(currentSearchText))
	    			atSearch = 0;

	    		doSearch();
	    	}  
	    });  
	    container.addView(nextButton);  
	      
	    Button closeButton = new Button(this);  
	    closeButton.setText("Close");
	    closeButton.setOnClickListener(new OnClickListener(){  
	    	@Override  
	    	public void onClick(View v){
	    		textContent.setText(virginText);
	    		container.removeAllViews();  
	    	}
    	}); 
	    container.addView(closeButton);  
	      
	    findBox.setOnKeyListener(new OnKeyListener(){  
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
		    	if((event.getAction() == KeyEvent.ACTION_DOWN) && ((keyCode == KeyEvent.KEYCODE_ENTER))){  
		    		if(!findBox.getText().toString().equals(currentSearchText))
		    			atSearch = 0;
		    		
		    		doSearch();
				}  
				return false;  
			}  
    	}); 
		findBox.setMinEms(30);  
		findBox.setSingleLine(true);
		container.addView(findBox);  
    }  

    void doSearch() {
    	View fc = mPager.getChildAt(lastPosition);
    	
		if(fc == null)
			return;
		
		textContent = (PaliTextView) fc.findViewById(R.id.main_text);
		scrollview = (ScrollView) fc.findViewById(R.id.scroll_text);
		if(textContent.getLayout() == null)
			return;

    	currentSearchText = findBox.getText().toString();
    	if(currentSearchText.length() == 0)
    		return;

    	currentSearchText = PaliUtils.toUni(currentSearchText);
    	findBox.setText(currentSearchText);

		InputMethodManager imm = (InputMethodManager) getSystemService(
			      Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(fc.getWindowToken(), 0);
    	
		String boxText = textContent.getText().toString();
		
		// next offset - this gives us the index in the whole text of the first instance after atSearch
		
		if(atSearch > 0) {
			String subText = boxText.substring(atSearch+currentSearchText.length());
				
			int offset = subText.indexOf(currentSearchText);

			// if not found, get first match (loop)
			
			if(offset < 1) {
				Toast.makeText(this, getString(R.string.search_from_start), Toast.LENGTH_SHORT).show();
				atSearch = boxText.indexOf(currentSearchText);
			}
			else
				atSearch =  atSearch+currentSearchText.length() + offset;
    	}
    	else
			atSearch = boxText.indexOf(currentSearchText);

		Log.d(TAG,"new atSearch: "+atSearch);

		if(atSearch == -1) {
			Toast.makeText(this, getString(R.string.search_no_match), Toast.LENGTH_SHORT).show();
			return;
		}
		
		// highlight
		
		Spannable spanText = Spannable.Factory.getInstance().newSpannable(virginText);
		spanText.setSpan(new BackgroundColorSpan(findColor), atSearch, atSearch+currentSearchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		textContent.setText(spanText);
		
		// scroll
		
		int jumpLine = textContent.getLayout().getLineForOffset(atSearch);
		int y=0;
		if(jumpLine > 2)
			y = textContent.getLayout().getLineTop(jumpLine - 2);
		else
			y = textContent.getLayout().getLineTop(0);
		scrollview.scrollTo(0, y);
		
    }
  
}