package com.enefsy.main;

/* Android packages */
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.content.SharedPreferences;

/* Facebook-specific packages */
import com.facebook.android.*;
import com.facebook.android.Facebook.DialogListener;


public class Main extends Activity implements DialogListener, OnClickListener {

	/* Image buttons for front screen */
	private ImageButton facebook_button;
	private ImageButton twitter_button;
	private ImageButton foursquare_button;
	
	/* Creates a Facebook Object with the Enefsy Facebook App ID */
	private Facebook facebookClient;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        facebook_button = (ImageButton) findViewById(R.id.facebook_button);
        twitter_button = (ImageButton) findViewById(R.id.twitter_button);
        foursquare_button = (ImageButton) findViewById(R.id.foursquare_button);

        facebook_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) 
        	{
        		Main.this.onClick(v);
        	}
        });

        twitter_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) 
        	{
        		Main.this.onClick(v);
        	}
        });

        foursquare_button.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) 
        	{
        		Main.this.onClick(v);
        	}
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookClient.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onError(DialogError e)
    {
        System.out.println("Error: " + e.getMessage());
    }
    
    @Override
    public void onCancel()
    {
		// TODO Auto-generated method stub
    }
    
    @Override
    public void onClick(View v)
    {
        /**********************************************************************
         * 								FACEBOOK
         *********************************************************************/
        if (v == facebook_button)        	
        {
        	facebookClient = new Facebook("287810317993311");        	

       		/* Attributes to check if user has granted permissions for facebook */
//       		String FILENAME = "AndroidSSO_data";
       	    final SharedPreferences mPrefs;
       	    
            try {
        		/* Last updated: 7/24/2012
                 * This is derived from the facebook developer's site on integrating with a 
                 * native android app: https://developers.facebook.com/docs/mobile/android/sso/
            	 * If the user clicks the facebook button, checks to see if user has granted
		    	 * permission to the app to publish streams and checkins to profile. If so,
		    	 * that permission is saved in an access token and can be references without 
		    	 * the user's permission in the future
		         */

            	mPrefs = getPreferences(MODE_PRIVATE);
                String access_token = mPrefs.getString("access_token", null);
                long expires = mPrefs.getLong("access_expires", 0);

                if(access_token != null) {
                    facebookClient.setAccessToken(access_token);
                }
                if(expires != 0) {
                    facebookClient.setAccessExpires(expires);
                }

                facebookClient.authorize(Main.this, new String[] { "publish_checkins, publish_stream" },
                	new DialogListener() {
                    	@Override
                        public void onComplete(Bundle values) {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putString("access_token", facebookClient.getAccessToken());
                            editor.putLong("access_expires", facebookClient.getAccessExpires());
                            editor.commit();
                            
                	        if (!values.containsKey("post_id"))
                	        {
                	            try
                	            {
                	            	// The following code will make an automatic status update
                	                Bundle parameters = new Bundle();
                	                parameters.putString("message", "Enefsy automatic location-based status updates...again");
                	                parameters.putString("place", "50937384449");
                	                parameters.putString("description", "test test test");
                	                facebookClient.request("me/feed", parameters, "POST");
                	            }
                	            catch (Exception e)
                	            {
                	                // TODO: handle exception
                	                System.out.println(e.getMessage());
                	            }
                	        }		
                        }
            
                        @Override
                        public void onFacebookError(FacebookError error) {}
            
                        @Override
                        public void onError(DialogError e) {}
            
                        @Override
                        public void onCancel() {}
                    });

            /* If the user can't post directly to facebook or doesn't grant our app permissions */
            } catch (Exception e1) {

            	/* Try redirecting to the native facebook app */
            	try {
            		Intent intent = new Intent("android.intent.category.LAUNCHER");
              		intent.setClassName("com.facebook.katana", "com.facebook.katana.LoginActivity");
              		startActivity(intent);
              		
           		/* Otherwise redirect to the mobile facebook site */
            	} catch (Exception e2) {
                    Intent intent = new Intent();
                    intent.setData(Uri.parse("https://m.facebook.com/?_rdr"));
                    startActivity(intent);
            	}
            }
        }
        
        /**********************************************************************
         * 								TWITTER
         *********************************************************************/
        /* The user clicks the Twitter button */
        else if (v == twitter_button) 
        {
            String message = "Your message to post";
            try {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setClassName("com.twitter.android","com.twitter.android.PostActivity");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(sharingIntent);
            } catch (Exception e) {
                Intent i = new Intent();
                i.putExtra(Intent.EXTRA_TEXT, message);
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://mobile.twitter.com/compose/tweet"));
                startActivity(i);
            }
        }
        
        /**********************************************************************
         * 								FOURSQUARE
         *********************************************************************/
        /* If the user clicks on the foursquare button */
        else if (v == foursquare_button) 
        {
            Intent intent = new Intent();
            intent.setData(Uri.parse("http://m.foursquare.com/user"));
            startActivity(intent);	
        }
    }
    
	@Override
	public void onComplete(Bundle values) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onFacebookError(FacebookError e) {
		// TODO Auto-generated method stub
		
	}
}