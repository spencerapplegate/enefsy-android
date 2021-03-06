package com.enefsy.foursquare;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.enefsy.foursquare.FoursquareDialog;
import com.enefsy.foursquare.FoursquareDialog.FsqDialogListener;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.Checkin;
import fi.foyt.foursquare.api.io.DefaultIOHandler;

import android.content.Context;
import android.util.Log;
import android.app.Activity;
import android.app.ProgressDialog;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;


public class FoursquareActivity extends Activity {

	private FoursquareApi mFoursquareApi;
	private FoursquareSession mSession;
	private FoursquareDialog mDialog;
	private FsqAuthListener mListener;
	private ProgressDialog mProgressDialog;
	private String mTokenUrl;
	private String mAccessToken;
	private String mCode;
	
	/**
	 * Callback url, as set in 'Manage OAuth Costumers' page (https://developer.foursquare.com/)
	 */	
	private static final String FOURSQUARE_CLIENT_ID = "C5PUFIZSZXKUPRP2PXFIA5C2UITZJ5N0KUNDXIUDDV1EVUX4";
	private static final String FOURSQUARE_CLIENT_SECRET = "UAKCECXTCJHGJ34VASJFSEY4LL2QSRAKNETWA4MWLDCFKJHS";
	private static final String FOURSQUARE_REDIRECT_URL = "http://www.enefsy.com";

	public static final String CALLBACK_URL = "http://www.enefsy.com";
	private static final String AUTH_URL = "https://foursquare.com/oauth2/authenticate?response_type=code";
	private static final String TOKEN_URL = "https://foursquare.com/oauth2/access_token?grant_type=authorization_code";	
	private static final String API_URL = "https://api.foursquare.com/v2";
	
	private static final String TAG = "FoursquareActivity";
	
    private String venueId;

    
	public FoursquareActivity(Context context) {
				
		mSession = new FoursquareSession(context);
		mAccessToken = mSession.getAccessToken();
		
		if (mAccessToken != null)
			initializeApi();

		mTokenUrl = TOKEN_URL + "&client_id=" + FOURSQUARE_CLIENT_ID + "&client_secret=" + FOURSQUARE_CLIENT_SECRET
						+ "&redirect_uri=" + CALLBACK_URL;
		
		String url = AUTH_URL + "&client_id=" + FOURSQUARE_CLIENT_ID + "&redirect_uri=" + CALLBACK_URL;

		FsqDialogListener listener = new FsqDialogListener() {
			@Override
			public void onComplete(String code) {
				mCode = code;
				getAccessToken();
			}
			
			@Override
			public void onError(String error) {
				mListener.onFail("Authorization failed");
			}
		};
		
		mDialog	= new FoursquareDialog(context, url, listener);
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setCancelable(true);
		
	}
	
	
	private void getAccessToken() {
		new FoursquareGetAccessTokenTask().execute();
	}
	
	
	private void saveAccessToken() {
		new FoursquareSaveAccessTokenTask().execute();	
	}
	
	
	public void initializeApi() {

		if (hasAccessToken()) {
			/* Create a foursquare API to deal with retrieving and posting info */
			mFoursquareApi = new FoursquareApi(FOURSQUARE_CLIENT_ID, 
												FOURSQUARE_CLIENT_SECRET, 
												FOURSQUARE_REDIRECT_URL,
												mAccessToken, 
												new DefaultIOHandler());
		}
	}
	
	
	public void checkIn(String venueId) {
		new FoursquareCheckinTask(mFoursquareApi, venueId).execute();
	}
	
	
	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}
	
	
	public void setListener(FsqAuthListener listener) {
		mListener = listener;
	}
	
	
	public String getUserName() {
		return mSession.getUsername();
	}
	
	
	public void authorizeAndCheckIn(String venueId) {
		this.venueId = venueId;
		mDialog.show();
	}	
	
	/* This is used to read data from the network about a user's 
	 * Foursquare profile */
	private String streamToString(InputStream is) throws IOException {
		String str  = "";
		
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;
			
			try {
				BufferedReader reader 	= new BufferedReader(new InputStreamReader(is));
				
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				
				reader.close();
			} finally {
				is.close();
			}
			
			str = sb.toString();
		}
		
		return str;
	}
	
	
	public interface FsqAuthListener {
		public abstract void onSuccess();
		public abstract void onFail(String error);
	}
	
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == 1) {
				if (msg.what == 0) {
					saveAccessToken();
				} else {
					mProgressDialog.dismiss();
					
					mListener.onFail("Failed to get access token");
				}
			} else {
				mProgressDialog.dismiss();
				
				mListener.onSuccess();
			}
		}
	};
	
	
	private class FoursquareGetAccessTokenTask extends AsyncTask<Uri, Void, Void> {
		
		protected void onPreExecute() {
			mProgressDialog.setMessage("Loading...");
			mProgressDialog.show();		
		}
		
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
		}
		
		@Override
		protected Void doInBackground(Uri... params) {

            try {
        				
				Log.i(TAG, "Getting access token");
				
				int what = 0;
				
				try {
					URL url = new URL(mTokenUrl + "&code=" + mCode);
					
					Log.i(TAG, "Opening URL " + url.toString());
					
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					
					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					
					urlConnection.connect();
					
					JSONObject jsonObj  = (JSONObject) new JSONTokener(streamToString(urlConnection.getInputStream())).nextValue();
		        	mAccessToken 		= jsonObj.getString("access_token");
		        	
		        	Log.i(TAG, "Got access token: " + mAccessToken);
				} catch (Exception ex) {
					what = 1;
					
					ex.printStackTrace();
				}
				
				/* Need to handle leaks eventually */
       			mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));	
       			
           } catch (Exception e) {
        	e.printStackTrace();
           }
            
            return null;
		}		
	}
	
	
	private class FoursquareSaveAccessTokenTask extends AsyncTask<Uri, Void, Void> {

		protected void onPreExecute() {
			mProgressDialog.setMessage("Saving settings...");
			mProgressDialog.show();
		}
				
		@Override
		protected Void doInBackground(Uri...params) {

            try {
				URL url = new URL(API_URL + "/users/self?oauth_token=" + mAccessToken);
			
				Log.d(TAG, "Opening URL " + url.toString());
				
				HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
				
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoInput(true);
				
				urlConnection.connect();
				
				String response		= streamToString(urlConnection.getInputStream());
				JSONObject jsonObj 	= (JSONObject) new JSONTokener(response).nextValue();
	       
				JSONObject resp		= (JSONObject) jsonObj.get("response");
				JSONObject user		= (JSONObject) resp.get("user");
				
				String firstName 	= user.getString("firstName");
	        	String lastName		= user.getString("lastName");
	        
	        	Log.i(TAG, "Got user name: " + firstName + " " + lastName);
	
	        	mSession.storeAccessToken(mAccessToken, firstName + " " + lastName);
            } 
            catch (Exception e) {
            	e.printStackTrace();
            }
			
            return null;
		}
		
		protected void onPostExecute(Void result) {
			// Initialize the Foursquare API using the user's authorization token
			// and check the user in
			mProgressDialog.dismiss();
    		initializeApi();
			checkIn(venueId);
	    }
	}
	
	
    private class FoursquareCheckinTask extends AsyncTask<Uri, Void, String> {

		private String apiStatusMsg;
		private FoursquareApi foursquareApi;
		private String venueId;
		
		public FoursquareCheckinTask(FoursquareApi foursquareApi, String venueId) {
			super();
			this.foursquareApi = foursquareApi;
			this.venueId = venueId;
		}
		
		protected void onPreExecute() {
			String msg = "Checking in...";
			mProgressDialog.setMessage(msg);
    		mProgressDialog.show();			
		}
		
		protected void onPostExecute(String s) {
			mProgressDialog.dismiss();
	    }

		@Override
		protected String doInBackground(Uri...params) {
			try {
				
        		Result<Checkin> result = this.foursquareApi.checkinsAdd(this.venueId, 
												null,"",null,null,null,null,null);

				if (result.getMeta().getCode()==200) {
					apiStatusMsg = "Thanks for checking in via Enefsy!";
				} else {
					apiStatusMsg = result.getMeta().getErrorDetail();
				}
			} catch (FoursquareApiException e) {
				e.printStackTrace();
			}
            return apiStatusMsg;
		}
    }
}